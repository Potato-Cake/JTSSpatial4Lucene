package cn.potatocake.lucene.spatial.geohash.tree;

import cn.potatocake.lucene.spatial.SpatialRelation;
import cn.potatocake.lucene.spatial.geohash.utils.GeoHash;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.apache.lucene.util.BytesRef;

/**
 * 基本栅格
 * <p>
 * @author @author Potato-Cake（potato_cake@163.com）
 */
public class Cell {

    private String token; // geohash值
    private byte[] bytes;
    protected byte shapeRel; // 栅格与图形关系
    private long bits;

    public Cell() {
    }

    public byte getShapeRel() {
        return shapeRel;
    }

    public String getToken() {
        return token;
    }

    public long longValue() {
        return bits;
    }

    public byte[] getBytes() {
        if (bytes == null) {
            bytes = token.getBytes(StandardCharsets.UTF_8);
        }
        return bytes;
    }

    public Cell reset(GeoHash geoHash, SpatialRelation _shapeRel) {
        token = geoHash.toBase32();
        bits = geoHash.longValue();
        shapeRel = _shapeRel.getByte();
        bytes = null;
        return this;
    }

    public Cell reset(BytesRef bytesRef) {
        this.bytes = Arrays.copyOf(bytesRef.bytes, bytesRef.length - 1);
        token = null;
        shapeRel = bytesRef.bytes[bytesRef.length - 1];
        return this;
    }

}
