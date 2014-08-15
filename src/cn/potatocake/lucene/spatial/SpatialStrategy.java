package cn.potatocake.lucene.spatial;

import com.vividsolutions.jts.geom.Geometry;
import org.apache.lucene.document.Field;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;

/**
 * 空间策略基类
 * <p>
 * @author Potato-Cake（potato_cake@163.com）
 */
public abstract class SpatialStrategy {

    /**
     * 根据图形创建lucene索引
     * <p>
     * @param shape
     *              <p>
     * @return
     */
    public abstract Field[] createIndexableFields(Geometry shape);

    /**
     * 根据图形创建距离排序，（未实现）
     * <p>
     * @param queryShape
     *                   <p>
     * @return
     */
    public abstract ValueSource makeDistanceValueSource(Geometry queryShape);

    /**
     * 创建图形过滤器
     * <p>
     * @param queryShape
     * @param relation
     *                   <p>
     * @return
     */
    public abstract Filter makeFilter(Geometry queryShape, SpatialRelation relation);

    /**
     * 创建图形查询器
     * <p>
     * @param queryShape
     * @param relation
     *                   <p>
     * @return
     */
    public Query makeQuery(Geometry queryShape, SpatialRelation relation) {
        return new ConstantScoreQuery(makeFilter(queryShape, relation));
    }
}
