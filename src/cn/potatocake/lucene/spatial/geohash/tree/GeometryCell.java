package cn.potatocake.lucene.spatial.geohash.tree;

import cn.potatocake.lucene.spatial.SpatialRelation;
import cn.potatocake.lucene.spatial.geohash.utils.GeoHash;
import cn.potatocake.lucene.spatial.geohash.utils.GeometryMaker;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 图形栅格类，可根据图形生成图形栅格列表及关系
 * <p>
 * @author @author Potato-Cake（potato_cake@163.com）
 */
public class GeometryCell implements Iterator<Cell> {

    public static final int POINT_CHAR_SIZE = 12;
    public static final int MAX_CHAR_SIZE = 4;
    public static final int MIN_CHAR_SIZE = 4;
    private final boolean isPoint;
    private final Envelope boundingBox;
    private final Geometry geometry;
    private final GeoHash bottomLeft;
    private final GeoHash topRight;
    private final Cell cell;
    private GeoHash current;
    private SpatialRelation relation;
    private boolean hasNextPoint;
    private GeometryCell subCell;
    private boolean isNextSub;
    private final int currentCharSize;
    private long min;
    private long max;
    private final Map<String, Boolean> map;

    public static GeometryCell getGeometryCell(Geometry geometry) {
        return new GeometryCell(geometry, MIN_CHAR_SIZE);
    }

    private GeometryCell(Geometry geometry, int charSize) {
        if (geometry == null) {
            throw new NullPointerException();
        }
        this.geometry = geometry;
        this.cell = new Cell();
        this.isPoint = (geometry instanceof Point);
        if (isPoint) {
            currentCharSize = POINT_CHAR_SIZE;
            Point point = (Point) geometry;
            this.current = GeoHash.getGeoHash(point.getY(), point.getX(), currentCharSize);
            hasNextPoint = true;
            this.boundingBox = null;
            this.bottomLeft = null;
            this.topRight = null;
            min = current.longValue();
            max = min;
            map = null;
        } else {
            if (geometry instanceof LineString) {
                currentCharSize = MAX_CHAR_SIZE;
                map = null;
            } else {
                currentCharSize = charSize;
                map = new HashMap<>();
            }
            this.boundingBox = geometry.getEnvelopeInternal();
            this.bottomLeft = GeoHash.getGeoHash(boundingBox.getMinY(), boundingBox.getMinX(), currentCharSize);
            this.topRight = GeoHash.getGeoHash(boundingBox.getMaxY(), boundingBox.getMaxX(), currentCharSize);
            this.current = bottomLeft;
            min = bottomLeft.longValue();
            max = topRight.longValue();
            if (!check()) {
                getNext();
            }
        }
    }

    private GeometryCell(Geometry geometry, Envelope _boundingBox, int charSize, Map<String, Boolean> m) {
        if (geometry == null) {
            throw new NullPointerException();
        }
        this.geometry = geometry;
        this.cell = new Cell();
        this.isPoint = (geometry instanceof Point);
        if (isPoint) {
            currentCharSize = POINT_CHAR_SIZE;
            Point point = (Point) geometry;
            this.current = GeoHash.getGeoHash(point.getY(), point.getX(), currentCharSize);
            hasNextPoint = true;
            this.boundingBox = null;
            this.bottomLeft = null;
            this.topRight = null;
            map = m;
        } else {
            if (geometry instanceof LineString) {
                currentCharSize = MAX_CHAR_SIZE;
                map = m;
            } else {
                currentCharSize = charSize;
                map = m;
            }
            this.boundingBox = _boundingBox;
            this.bottomLeft = GeoHash.getGeoHash(boundingBox.getMinY(), boundingBox.getMinX(), currentCharSize);
            this.topRight = GeoHash.getGeoHash(boundingBox.getMaxY(), boundingBox.getMaxX(), currentCharSize);
            this.current = bottomLeft;
            if (!check()) {
                getNext();
            }
        }
    }

    public long getMin() {
        return min;
    }

    public long getMax() {
        return max;
    }

    public Envelope getBoundingBox() {
        return boundingBox;
    }

    public GeoHash getBottomLeft() {
        return bottomLeft;
    }

    public GeoHash getTopRight() {
        return topRight;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    @Override
    public boolean hasNext() {
        if (isPoint) {
            return hasNextPoint;
        }
        return checkSubCell() || current.compareTo(topRight) <= 0;
    }

    @Override
    public Cell next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        if (isPoint) {
            hasNextPoint = false;
            return cell.reset(current, SpatialRelation.WITHIN);
        }
        if (checkSubCell()) {
            return subCell.next();
        }
        cell.reset(current, relation);
        getNext();
        return cell;
    }

    private void getNext() {
        while (current.compareTo(topRight) <= 0) {
            current = current.next();
            if (check()) {
                if (map != null) {
                    map.put(current.toBase32(), true);
                }
                break;
            }
        }
    }

    private boolean checkSubCell() {
        if (subCell != null) {
            if (subCell.hasNext()) {
                return true;
            } else {
                if (isNextSub) {
                    subCell = new GeometryCell(geometry, current.getBoundingBox(), currentCharSize + 1, map);
                    isNextSub = false;
                    getNext();
                    return subCell.hasNext();
                } else {
                    subCell = null;
                    return false;
                }
            }
        }
        return false;
    }

    private boolean isNeed() {
        return relation == SpatialRelation.CONTAINS || (currentCharSize == MAX_CHAR_SIZE && relation == SpatialRelation.INTERSECTS);
    }

    private boolean check() {
        if (map != null) {
            if (map.containsKey(current.toBase32())) {
                return false;
            }
        }
        Geometry g = GeometryMaker.toGeometry(current.getBoundingBox());
        if (geometry.contains(g)) {
            relation = SpatialRelation.CONTAINS;
        } else if (geometry.intersects(g)) {
            relation = SpatialRelation.INTERSECTS;
            if (currentCharSize < MAX_CHAR_SIZE) {
                if (subCell != null) {
                    isNextSub = true;
                    return true;
                }
                subCell = new GeometryCell(geometry, current.getBoundingBox(), currentCharSize + 1, map);
                getNext();
                return true;
            }
        } else if (geometry.within(g)) {
            relation = SpatialRelation.WITHIN;
        } else {
            relation = SpatialRelation.DISJOINT;
        }
        return isNeed();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public void remain() {
        if (isPoint) {
            hasNextPoint = true;
        } else {
            current = bottomLeft;
            subCell = null;
            relation = null;
            if (map != null) {
                map.clear();
            }
        }
    }
}
