package cn.potatocake.lucene.spatial.geohash;

import cn.potatocake.lucene.spatial.SpatialRelation;
import cn.potatocake.lucene.spatial.SpatialStrategy;
import cn.potatocake.lucene.spatial.geohash.filter.AbstractSpatialFilter.TokenTree;
import cn.potatocake.lucene.spatial.geohash.filter.ContainsSpatialFilter;
import cn.potatocake.lucene.spatial.geohash.filter.DisjointSpatialFilter;
import cn.potatocake.lucene.spatial.geohash.filter.IntersectsSpatialFilter;
import cn.potatocake.lucene.spatial.geohash.filter.WithinSpatialFilter;
import cn.potatocake.lucene.spatial.geohash.tree.Cell;
import cn.potatocake.lucene.spatial.geohash.tree.GeometryCell;
import com.vividsolutions.jts.geom.Geometry;
import java.util.Iterator;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.search.Filter;

/**
 * 基于Geohash的空间策略实现类
 * <p>
 * @author Potato-Cake（potato_cake@163.com）
 */
public class GeohashTreeSpatialStrategy extends SpatialStrategy {

    public static final String FIELD_NAME = "_______SHAPE";
    public static final String CELL_FIELD_NAME = FIELD_NAME + "___CELL___"; // 栅格索引名称
    public static final String SHAPE_FIELD_NAME = FIELD_NAME + "___WKT___"; // 图形索引名称

    private static final FieldType FIELD_CELL_TYPE = new FieldType();

    static {
        FIELD_CELL_TYPE.setIndexed(true);
        FIELD_CELL_TYPE.setTokenized(true);
        FIELD_CELL_TYPE.setOmitNorms(true);
        FIELD_CELL_TYPE.setStored(false);
        FIELD_CELL_TYPE.setIndexOptions(FieldInfo.IndexOptions.DOCS_ONLY);
        FIELD_CELL_TYPE.freeze();
    }

    @Override
    public Field[] createIndexableFields(Geometry shape) {
        Field cellField = new Field(CELL_FIELD_NAME,
                new CellTokenStream(GeometryCell.getGeometryCell(shape)), FIELD_CELL_TYPE);
        Field shapeField = new StringField(SHAPE_FIELD_NAME, shape.toText(), Field.Store.YES);
        return new Field[]{cellField, shapeField};
    }

    @Override
    public ValueSource makeDistanceValueSource(Geometry queryShape) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Filter makeFilter(Geometry queryShape, SpatialRelation relation) {
        TokenTree tokenTree = new TokenTree();
        GeometryCell geometryCell = GeometryCell.getGeometryCell(queryShape);
        while (geometryCell.hasNext()) {
            Cell cell = geometryCell.next();
            tokenTree.setToken(cell.getBytes(), cell.getShapeRel());
        }
        Filter shapFilter = null;
        if (relation == SpatialRelation.WITHIN) {
            shapFilter = new WithinSpatialFilter(queryShape);
        } else if (relation == SpatialRelation.CONTAINS) {
            shapFilter = new ContainsSpatialFilter(queryShape);
        } else if (relation == SpatialRelation.INTERSECTS) {
            shapFilter = new IntersectsSpatialFilter(queryShape);
        } else if (relation == SpatialRelation.DISJOINT) {
            shapFilter = new DisjointSpatialFilter(queryShape);
        } else {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        return shapFilter;
    }

    final static class CellTokenStream extends TokenStream {

        private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

        private Iterator<Cell> iter = null;

        public CellTokenStream(Iterator<Cell> tokens) {
            this.iter = tokens;
        }

        @Override
        public boolean incrementToken() {
            clearAttributes();
            if (iter.hasNext()) {
                Cell cell = iter.next();
                termAtt.append(cell.getToken() + (char) (cell.getShapeRel()));
                return true;
            }
            return false;
        }

    }
}
