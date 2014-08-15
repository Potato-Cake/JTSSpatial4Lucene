package cn.potatocake.lucene.spatial.geohash.utils;

import cn.potatocake.lucene.spatial.geohash.GeohashTreeSpatialStrategy;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.apache.lucene.document.Document;

/**
 * 图形构建工具类
 * <p>
 * @author Potato-Cake（potato_cake@163.com）
 */
public class GeometryMaker {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final WKTReader WKT_READER = new WKTReader();

    /**
     * 从lucene文档获取图形
     * <p>
     * @param doc
     *            <p>
     * @return
     */
    public static Geometry fromDocument(Document doc) {
        try {
            return readWKT(doc.get(GeohashTreeSpatialStrategy.SHAPE_FIELD_NAME));
        } catch (ParseException ex) {
            return null;
        }
    }

    public static Geometry readWKT(String wkt) throws ParseException {
        return WKT_READER.read(wkt);
    }

    public static Geometry toGeometry(Envelope envelope) {
        return GEOMETRY_FACTORY.toGeometry(envelope);
    }

    public static Point makePoint(double x, double y) {
        return GEOMETRY_FACTORY.createPoint(makeCoordinate(x, y));
    }

    public static Coordinate makeCoordinate(double x, double y) {
        return new Coordinate(x, y);
    }

    public static LineString makeLineString(Coordinate[] points) {
        return GEOMETRY_FACTORY.createLineString(points);
    }

    public static Geometry makeRectangle(double minX, double maxX, double minY, double maxY) {
        return toGeometry(new Envelope(minX, maxX, minY, maxY));
    }

    public static Geometry makeBuffer(Geometry center, double distance) {
        return center.buffer(distance * DistanceUtils.KM_TO_DEG);
    }

}
