package cn.potatocake.lucene.spatial.geohash.filter;

import cn.potatocake.lucene.spatial.SpatialRelation;
import cn.potatocake.lucene.spatial.geohash.utils.GeometryMaker;
import com.vividsolutions.jts.geom.Geometry;
import java.io.IOException;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.util.Bits;

/**
 * 包含过滤器
 * <p>
 * @author @author Potato-Cake（potato_cake@163.com）
 */
public class ContainsSpatialFilter extends AbstractSpatialFilter {

    public ContainsSpatialFilter(Geometry geometry) {
        super(geometry);
    }

    @Override
    protected SpatialRelation getSpatialRelation() {
        return SpatialRelation.CONTAINS;
    }

    @Override
    public Visitor getVisitor(AtomicReaderContext context, Bits acceptDocs, Geometry geometry) throws IOException {
        return new Visitor(context, acceptDocs, geometry) {

            @Override
            protected void finish() throws IOException {
            }

            @Override
            protected boolean isNeed(int docid) throws IOException {
                Document document = context.reader().document(docid);
                Geometry g = GeometryMaker.fromDocument(document);
                return geometry.contains(g);
            }

            @Override
            protected void start() throws IOException {
            }

            @Override
            protected boolean visit(byte cellRelation, byte relation) throws IOException {
                if (relation == SpatialRelation.CONTAINS.getByte()) {
                    if (cellRelation == SpatialRelation.WITHIN.getByte()) {
                        collectDocs();
                    } else {
                        checkDocs();
                    }
                    return true;
                } else if (relation == SpatialRelation.INTERSECTS.getByte()) {
                    checkDocs();
                    return true;
                }
                return false;
            }

        };
    }

}
