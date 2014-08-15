JTSSpatial4Lucene
=================

基于jts实现的lucene图形检索，使用方式与lucene-spatial基本相同。

使用说明：
------------------------

### 创建索引：
    SpatialStrategy strategy = new GeohashTreeSpatialStrategy();
    DirectoryWriter writer = ...;
    Document doc = new Document();
    for (IndexableField f : strategy.createIndexableFields(GeometryMaker.makePoint(lon, lat))) {
      doc.add(f);
    }
    writer.addDocument(doc);
### 检索：
    SpatialStrategy strategy = new GeohashTreeSpatialStrategy();
    IndexSearcher searcher = ...;
    TopScoreDocCollector collector = TopScoreDocCollector.create(100, false);
    // 半径5公里的圆
    Geometry geometry = GeometryMaker.makeCircle(GeometryMaker.makeBuffer(116.404844, 39.922904), 5);
    // 相交
    Filter filter = strategy.makeFilter(geometry, SpatialRelation.INTERSECTS);
    searcher.search(new MatchAllDocsQuery(), filter, collector);
    TopDocs tds = collector.topDocs(0, 10);
    ScoreDoc[] hits = tds.scoreDocs;
    for (ScoreDoc sd : hits) {
        Document hitDoc = searcher.doc(sd.doc);
        Geometry shape = GeometryMaker.fromDocument(hitDoc);
        ...
    }
