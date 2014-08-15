package cn.potatocake.lucene.spatial.geohash.filter;

import cn.potatocake.lucene.spatial.SpatialRelation;
import cn.potatocake.lucene.spatial.geohash.utils.GeometryMaker;
import com.vividsolutions.jts.geom.Geometry;
import java.io.IOException;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.util.Bits;

/**
 * 相交过滤器
 * <p>
 * @author @author Potato-Cake（potato_cake@163.com）
 */
public class IntersectsSpatialFilter extends AbstractSpatialFilter {

    public IntersectsSpatialFilter(Geometry geometry) {
        super(geometry);
    }

    @Override
    protected SpatialRelation getSpatialRelation() {
        return SpatialRelation.INTERSECTS;
    }

    @Override
    public Visitor getVisitor(AtomicReaderContext context, Bits acceptDocs, final Geometry geometry) throws IOException {
        return new Visitor(context, acceptDocs, geometry) {

            @Override
            protected void finish() throws IOException {
            }

            @Override
            protected boolean isNeed(int docid) throws IOException {
                Document document = context.reader().document(docid);
                Geometry g = GeometryMaker.fromDocument(document);
                return geometry.intersects(g);
            }

            @Override
            protected void start() throws IOException {
            }

            @Override
            protected boolean visit(byte cellRelation, byte relation) throws IOException {
                if (relation == SpatialRelation.CONTAINS.getByte()) {
                    collectDocs();
                    return true;
                } else if (relation == SpatialRelation.INTERSECTS.getByte()) {
                    if (cellRelation == SpatialRelation.CONTAINS.getByte()) {
                        collectDocs();
                    } else {
                        checkDocs();
                    }
                    return true;
                }
                return false;
            }
        };
    }

}
