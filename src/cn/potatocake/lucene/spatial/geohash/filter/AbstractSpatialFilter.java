package cn.potatocake.lucene.spatial.geohash.filter;

import cn.potatocake.lucene.spatial.SpatialRelation;
import cn.potatocake.lucene.spatial.geohash.GeohashTreeSpatialStrategy;
import cn.potatocake.lucene.spatial.geohash.tree.Cell;
import cn.potatocake.lucene.spatial.geohash.tree.GeometryCell;
import com.vividsolutions.jts.geom.Geometry;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FixedBitSet;

/**
 * 图形过滤器基类
 * <p>
 * @author @author Potato-Cake（potato_cake@163.com）
 */
public abstract class AbstractSpatialFilter extends Filter {

    protected final Geometry geometry;
    protected final TokenTree tokenTree;
    private final Map<Integer, Boolean> idChecked = new WeakHashMap<>();

    public AbstractSpatialFilter(Geometry geometry) {
        this.geometry = geometry;
        tokenTree = new TokenTree();
        GeometryCell geometryCell = GeometryCell.getGeometryCell(geometry);
        while (geometryCell.hasNext()) {
            Cell cell = geometryCell.next();
            tokenTree.setToken(cell.getBytes(), cell.getShapeRel());
        }
    }

    @Override
    public DocIdSet getDocIdSet(AtomicReaderContext context, Bits acceptDocs) throws IOException {
        return getVisitor(context, acceptDocs, geometry).getDocIdSet();
    }

    public abstract Visitor getVisitor(AtomicReaderContext context, Bits acceptDocs, Geometry geometry) throws IOException;

    protected abstract SpatialRelation getSpatialRelation();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!getClass().equals(o.getClass())) {
            return false;
        }

        AbstractSpatialFilter that = (AbstractSpatialFilter) o;

        if (!getSpatialRelation().equals(that.getSpatialRelation())) {
            return false;
        }
        return geometry.equals(that.geometry);
    }

    @Override
    public int hashCode() {
        int result = geometry.hashCode();
        result = 31 * result + getSpatialRelation().getByte();
        return result;
    }

    abstract class Visitor {

        protected final AtomicReaderContext context;
        protected final Geometry geometry;
        protected final int maxDoc;
        protected Bits acceptDocs;
        protected TermsEnum termsEnum;
        protected DocsEnum docsEnum;
        private BytesRef thisTerm;
        protected FixedBitSet bitSet;

        public Visitor(AtomicReaderContext context, Bits acceptDocs, Geometry geometry) throws IOException {
            this.context = context;
            this.geometry = geometry;
            AtomicReader reader = context.reader();
            this.acceptDocs = acceptDocs;
            this.maxDoc = reader.maxDoc();
            Terms terms = reader.terms(GeohashTreeSpatialStrategy.CELL_FIELD_NAME);
            if (terms != null) {
                this.termsEnum = terms.iterator(null);
            }
        }

        public DocIdSet getDocIdSet() throws IOException {
            if (termsEnum == null) {
                return null;
            }
            if ((thisTerm = termsEnum.next()) == null) {
                return null;
            }
            bitSet = new FixedBitSet(maxDoc);
            start();
            while (thisTerm != null) {
                byte relation = tokenTree.checkToken(thisTerm.bytes);
                visit(thisTerm.bytes[thisTerm.length - 1], relation);
                if ((thisTerm = termsEnum.next()) == null) {
                    break;
                }
            }
            finish();
            return bitSet;
        }

        protected void collectDocs() throws IOException {
            docsEnum = termsEnum.docs(acceptDocs, docsEnum, DocsEnum.FLAG_NONE);
            int docid;
            while ((docid = docsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
                bitSet.set(docid);
            }
        }

        protected void checkDocs() throws IOException {
            docsEnum = termsEnum.docs(acceptDocs, docsEnum, DocsEnum.FLAG_NONE);
            int docid;
            while ((docid = docsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
                if (idChecked.containsKey(docid)) {
                    continue;
                }
                boolean r = isNeed(docid);
                idChecked.put(docid, r);
                if (r) {
                    bitSet.set(docid);
                }
            }
        }

        protected abstract boolean isNeed(int docid) throws IOException;

        protected abstract void start() throws IOException;

        protected abstract void finish() throws IOException;

        protected abstract boolean visit(byte cellRelation, byte relation) throws IOException;
    }

    public static class TokenTree {

        private static final byte MIN_NUM_BYTE = '0';
        private static final byte MIN_CHAR_BYTE = 'b' - 10;
        private final TokenTree[] tree = new TokenTree[36];
        private byte rel = Byte.MIN_VALUE;

        public void setToken(byte[] token, byte rel) {
            TokenTree[] ttree = tree;
            for (int i = 0; i < token.length; i++) {
                int pos = pos(token[i]);
                TokenTree t = ttree[pos];
                if (t == null) {
                    t = new TokenTree();
                    ttree[pos] = t;
                }
                if (i == (token.length - 1)) {
                    t.rel = rel;
                }
                ttree = t.tree;
            }
        }

        public byte checkToken(byte[] token) {
            byte result = SpatialRelation.DISJOINT.getByte();
            TokenTree[] ttree = tree;
            for (byte b : token) {
                TokenTree t = ttree[pos(b)];
                if (t != null) {
                    if (t.rel != Byte.MIN_VALUE) {
                        result = t.rel;
                        break;
                    }
                } else {
                    break;
                }
                ttree = t.tree;
            }
            return result;
        }

        private int pos(byte b) {
            if (b > MIN_CHAR_BYTE) {
                return b - MIN_CHAR_BYTE;
            } else {
                return b - MIN_NUM_BYTE;
            }
        }

        public byte getRel() {
            return rel;
        }

        @Override
        public String toString() {
            String s;
            if (rel != Byte.MIN_VALUE) {
                s = "" + ((char) rel);
            } else {
                s = "c";
            }
            s += Arrays.toString(tree);
            return s;
        }

    }
}
