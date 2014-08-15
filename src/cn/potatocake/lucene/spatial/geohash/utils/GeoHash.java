package cn.potatocake.lucene.spatial.geohash.utils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * GeoHash工具类
 * <p>
 * @author @author Potato-Cake（potato_cake@163.com）
 */
public final class GeoHash implements Comparable<GeoHash> {

    private static final double[][] WORLD = {{-90.0, 90.0}, {-180.0, 180.0}};
//    private static final double[][] CHINA = {{0.0, 60.0}, {70.0, 140.0}};
    private static final int[] BITS = {16, 8, 4, 2, 1};
    private static final int BASE32_BITS = 5;
    public static final long FIRST_BIT_FLAGGED = 0x8000000000000000l;
    private static final char[] base32 = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'b', 'c', 'd', 'e', 'f',
        'g', 'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
    private static final Map<Character, Integer> decodeMap = new HashMap<>();

    static {
        int sz = base32.length;
        for (int i = 0; i < sz; i++) {
            decodeMap.put(base32[i], i);
        }
    }

    private long bits = 0;
    private String token;
    private Coordinate point;
    private Envelope boundingBox;
    private byte significantBits = 0;

    protected GeoHash() {
    }

    public static GeoHash getGeoHash(double latitude, double longitude, int charSize) {
        return new GeoHash(latitude, longitude, BASE32_BITS * charSize);
    }

    public static GeoHash fromBinaryString(String binaryString) {
        GeoHash geohash = new GeoHash();
        for (int i = 0; i < binaryString.length(); i++) {
            if (binaryString.charAt(i) == '1') {
                geohash.addOnBitToEnd();
            } else if (binaryString.charAt(i) == '0') {
                geohash.addOffBitToEnd();
            } else {
                throw new IllegalArgumentException(binaryString + " is not a valid geohash as a binary string");
            }
        }
        geohash.bits <<= (64 - geohash.significantBits);
        long[] latitudeBits = geohash.getRightAlignedLatitudeBits();
        long[] longitudeBits = geohash.getRightAlignedLongitudeBits();
        return geohash.recombineLatLonBitsToHash(latitudeBits, longitudeBits);
    }

    public static GeoHash fromGeohashString(String geohash) {
        double[] latitudeRange = Arrays.copyOf(WORLD[0], 2);
        double[] longitudeRange = Arrays.copyOf(WORLD[1], 2);

        boolean isEvenBit = true;
        GeoHash hash = new GeoHash();

        for (int i = 0; i < geohash.length(); i++) {
            int cd = decodeMap.get(geohash.charAt(i));
            for (int j = 0; j < BASE32_BITS; j++) {
                int mask = BITS[j];
                if (isEvenBit) {
                    divideRangeDecode(hash, longitudeRange, (cd & mask) != 0);
                } else {
                    divideRangeDecode(hash, latitudeRange, (cd & mask) != 0);
                }
                isEvenBit = !isEvenBit;
            }
        }

        double latitude = (latitudeRange[0] + latitudeRange[1]) / 2;
        double longitude = (longitudeRange[0] + longitudeRange[1]) / 2;
        hash.point = new Coordinate(latitude, longitude);
        setBoundingBox(hash, latitudeRange, longitudeRange);
        hash.bits <<= (64 - hash.significantBits);
        return hash;
    }

    public static GeoHash fromLongValue(long hashVal, int charSize) {
        double[] latitudeRange = Arrays.copyOf(WORLD[0], 2);
        double[] longitudeRange = Arrays.copyOf(WORLD[1], 2);

        boolean isEvenBit = true;
        GeoHash hash = new GeoHash();

        String binaryString = Long.toBinaryString(hashVal);
        while (binaryString.length() < 64) {
            binaryString = "0" + binaryString;
        }
        for (int j = 0; j < BASE32_BITS * charSize; j++) {
            if (isEvenBit) {
                divideRangeDecode(hash, longitudeRange, binaryString.charAt(j) != '0');
            } else {
                divideRangeDecode(hash, latitudeRange, binaryString.charAt(j) != '0');
            }
            isEvenBit = !isEvenBit;
        }

        double latitude = (latitudeRange[0] + latitudeRange[1]) / 2;
        double longitude = (longitudeRange[0] + longitudeRange[1]) / 2;

        hash.point = new Coordinate(latitude, longitude);
        setBoundingBox(hash, latitudeRange, longitudeRange);
        hash.bits <<= (64 - hash.significantBits);
        return hash;
    }

    private GeoHash(double latitude, double longitude, int desiredPrecision) {
        point = new Coordinate(latitude, longitude);
        desiredPrecision = Math.min(desiredPrecision, 64);
        boolean isEvenBit = true;
        double[] latitudeRange = Arrays.copyOf(WORLD[0], 2);
        double[] longitudeRange = Arrays.copyOf(WORLD[1], 2);

        while (significantBits < desiredPrecision) {
            if (isEvenBit) {
                divideRangeEncode(longitude, longitudeRange);
            } else {
                divideRangeEncode(latitude, latitudeRange);
            }
            isEvenBit = !isEvenBit;
        }
        setBoundingBox(this, latitudeRange, longitudeRange);
        bits <<= (64 - desiredPrecision);
    }

    private static void setBoundingBox(GeoHash hash, double[] latitudeRange, double[] longitudeRange) {
        hash.boundingBox = new Envelope(longitudeRange[0], longitudeRange[1], latitudeRange[0], latitudeRange[1]);
    }

    public GeoHash next(int step) {
        return fromOrd(ord() + step, significantBits / BASE32_BITS);
    }

    public GeoHash next() {
        return next(1);
    }

    public GeoHash prev() {
        return next(-1);
    }

    public long ord() {
        int insignificantBits = 64 - significantBits;
        return bits >> insignificantBits;
    }

    public int getCharacterPrecision() {
        if (significantBits % 5 != 0) {
            throw new IllegalStateException(
                    "precision of GeoHash is not divisble by 5: " + this);
        }
        return significantBits / 5;
    }

    public static GeoHash fromOrd(long ord, int charSize) {
        int insignificantBits = 64 - BASE32_BITS * charSize;
        return fromLongValue(ord << insignificantBits, charSize);
    }

    public static long stepsBetween(GeoHash one, GeoHash two) {
        if (one.significantBits() != two.significantBits()) {
            throw new IllegalArgumentException(
                    "It is only valid to compare the number of steps between two hashes if they have the same number of significant bits");
        }
        return two.ord() - one.ord();
    }

    private void divideRangeEncode(double value, double[] range) {
        double mid = (range[0] + range[1]) / 2;
        if (value >= mid) {
            addOnBitToEnd();
            range[0] = mid;
        } else {
            addOffBitToEnd();
            range[1] = mid;
        }
    }

    private static void divideRangeDecode(GeoHash hash, double[] range, boolean b) {
        double mid = (range[0] + range[1]) / 2;
        if (b) {
            hash.addOnBitToEnd();
            range[0] = mid;
        } else {
            hash.addOffBitToEnd();
            range[1] = mid;
        }
    }

    public GeoHash[] getAdjacent() {
        GeoHash northern = getNorthernNeighbour();
        GeoHash eastern = getEasternNeighbour();
        GeoHash southern = getSouthernNeighbour();
        GeoHash western = getWesternNeighbour();
        return new GeoHash[]{northern, northern.getEasternNeighbour(), eastern, southern.getEasternNeighbour(),
            southern,
            southern.getWesternNeighbour(), western, northern.getWesternNeighbour()};
    }

    public int significantBits() {
        return significantBits;
    }

    public long longValue() {
        return bits;
    }

    public String toBase32() {
        if (significantBits % 5 != 0) {
            return "";
        }
        if (token == null) {
            StringBuilder buf = new StringBuilder();
            long firstFiveBitsMask = 0xf800000000000000l;
            long bitsCopy = bits;
            int partialChunks = (int) Math.ceil(((double) significantBits / 5));
            for (int i = 0; i < partialChunks; i++) {
                int pointer = (int) ((bitsCopy & firstFiveBitsMask) >>> 59);
                buf.append(base32[pointer]);
                bitsCopy <<= 5;
            }
            token = buf.toString();
        }
        return token;
    }

    public boolean within(GeoHash boundingBox) {
        return (bits & boundingBox.mask()) == boundingBox.bits;
    }

    public boolean contains(Coordinate point) {
        return boundingBox.contains(point);
    }

    public Coordinate getPoint() {
        return point;
    }

    public Coordinate getBoundingBoxCenterPoint() {
        return boundingBox.centre();
    }

    public Envelope getBoundingBox() {
        return boundingBox;
    }

    public boolean enclosesCircleAroundPoint(Coordinate point, double radius) {
        return false;
    }

    protected GeoHash recombineLatLonBitsToHash(long[] latBits, long[] lonBits) {
        GeoHash hash = new GeoHash();
        boolean isEvenBit = false;
        latBits[0] <<= (64 - latBits[1]);
        lonBits[0] <<= (64 - lonBits[1]);
        double[] latitudeRange = Arrays.copyOf(WORLD[0], 2);
        double[] longitudeRange = Arrays.copyOf(WORLD[1], 2);

        for (int i = 0; i < latBits[1] + lonBits[1]; i++) {
            if (isEvenBit) {
                divideRangeDecode(hash, latitudeRange, (latBits[0] & FIRST_BIT_FLAGGED) == FIRST_BIT_FLAGGED);
                latBits[0] <<= 1;
            } else {
                divideRangeDecode(hash, longitudeRange, (lonBits[0] & FIRST_BIT_FLAGGED) == FIRST_BIT_FLAGGED);
                lonBits[0] <<= 1;
            }
            isEvenBit = !isEvenBit;
        }
        hash.bits <<= (64 - hash.significantBits);
        setBoundingBox(hash, latitudeRange, longitudeRange);
        hash.point = hash.boundingBox.centre();
        return hash;
    }

    public GeoHash getNorthernNeighbour() {
        long[] latitudeBits = getRightAlignedLatitudeBits();
        long[] longitudeBits = getRightAlignedLongitudeBits();
        latitudeBits[0] += 1;
        latitudeBits[0] = maskLastNBits(latitudeBits[0], latitudeBits[1]);
        return recombineLatLonBitsToHash(latitudeBits, longitudeBits);
    }

    public GeoHash getSouthernNeighbour() {
        long[] latitudeBits = getRightAlignedLatitudeBits();
        long[] longitudeBits = getRightAlignedLongitudeBits();
        latitudeBits[0] -= 1;
        latitudeBits[0] = maskLastNBits(latitudeBits[0], latitudeBits[1]);
        return recombineLatLonBitsToHash(latitudeBits, longitudeBits);
    }

    public GeoHash getEasternNeighbour() {
        long[] latitudeBits = getRightAlignedLatitudeBits();
        long[] longitudeBits = getRightAlignedLongitudeBits();
        longitudeBits[0] += 1;
        longitudeBits[0] = maskLastNBits(longitudeBits[0], longitudeBits[1]);
        return recombineLatLonBitsToHash(latitudeBits, longitudeBits);
    }

    public GeoHash getWesternNeighbour() {
        long[] latitudeBits = getRightAlignedLatitudeBits();
        long[] longitudeBits = getRightAlignedLongitudeBits();
        longitudeBits[0] -= 1;
        longitudeBits[0] = maskLastNBits(longitudeBits[0], longitudeBits[1]);
        return recombineLatLonBitsToHash(latitudeBits, longitudeBits);
    }

    protected long[] getRightAlignedLatitudeBits() {
        long copyOfBits = bits << 1;
        long value = extractEverySecondBit(copyOfBits, getNumberOfLatLonBits()[0]);
        return new long[]{value, getNumberOfLatLonBits()[0]};
    }

    protected long[] getRightAlignedLongitudeBits() {
        long copyOfBits = bits;
        long value = extractEverySecondBit(copyOfBits, getNumberOfLatLonBits()[1]);
        return new long[]{value, getNumberOfLatLonBits()[1]};
    }

    private long extractEverySecondBit(long copyOfBits, int numberOfBits) {
        long value = 0;
        for (int i = 0; i < numberOfBits; i++) {
            if ((copyOfBits & FIRST_BIT_FLAGGED) == FIRST_BIT_FLAGGED) {
                value |= 0x1;
            }
            value <<= 1;
            copyOfBits <<= 2;
        }
        value >>>= 1;
        return value;
    }

    protected int[] getNumberOfLatLonBits() {
        if (significantBits % 2 == 0) {
            return new int[]{significantBits / 2, significantBits / 2};
        } else {
            return new int[]{significantBits / 2, significantBits / 2 + 1};
        }
    }

    protected final void addOnBitToEnd() {
        significantBits++;
        bits <<= 1;
        bits = bits | 0x1;
    }

    protected final void addOffBitToEnd() {
        significantBits++;
        bits <<= 1;
    }

    @Override
    public String toString() {
        if (significantBits % 5 == 0) {
            return String.format("%s -> %s -> %s", Long.toBinaryString(bits), boundingBox, toBase32());
        } else {
            return String.format("%s -> %s, bits: %d", Long.toBinaryString(bits), boundingBox, significantBits);
        }
    }

    public String toBinaryString() {
        StringBuilder bui = new StringBuilder();
        long bitsCopy = bits;
        for (int i = 0; i < significantBits; i++) {
            if ((bitsCopy & FIRST_BIT_FLAGGED) == FIRST_BIT_FLAGGED) {
                bui.append('1');
            } else {
                bui.append('0');
            }
            bitsCopy <<= 1;
        }
        return bui.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof GeoHash) {
            GeoHash other = (GeoHash) obj;
            if (other.significantBits == significantBits && other.bits == bits) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int f = 17;
        f = 31 * f + (int) (bits ^ (bits >>> 32));
        f = 31 * f + significantBits;
        return f;
    }

    private long mask() {
        if (significantBits == 0) {
            return 0;
        } else {
            long value = FIRST_BIT_FLAGGED;
            value >>= (significantBits - 1);
            return value;
        }
    }

    private long maskLastNBits(long value, long n) {
        long mask = 0xffffffffffffffffl;
        mask >>>= (64 - n);
        return value & mask;
    }

    @Override
    public int compareTo(GeoHash o) {
        return new Long(bits).compareTo(o.bits);
    }
}
