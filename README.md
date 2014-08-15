JTSSpatial4Lucene
=================

基于jts实现的lucene图形检索，使用方式与lucene-spatial基本相同。

创建索引：
------------------------

### SpatialStrategy strategy = new GeohashTreeSpatialStrategy();
    DirectoryWriter writer = ...
    Document doc = new Document();
    for (IndexableField f : strategy.createIndexableFields(GeometryMaker.makePoint(lon, lat))) {
      doc.add(f);
    }
    writer.addDocument(doc);
