package cn.potatocake.lucene.spatial.geohash.filter;

import cn.potatocake.lucene.spatial.SpatialRelation;
import com.vividsolutions.jts.geom.Geometry;
import java.io.IOException;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.util.Bits;

/**
 * 脱离过滤器
 * 
 * @author @author Potato-Cake（potato_cake@163.com）
 */
public class DisjointSpatialFilter extends AbstractSpatialFilter {

    public DisjointSpatialFilter(Geometry geometry) {
        super(geometry);
    }
    
    @Override
    protected SpatialRelation getSpatialRelation() {
        return SpatialRelation.DISJOINT;
    }

    @Override
    public Visitor getVisitor(AtomicReaderContext context, Bits acceptDocs, Geometry geometry) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

}
