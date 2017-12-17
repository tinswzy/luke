package org.apache.lucene.luke.models.search;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleDocValuesField;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FloatDocValuesField;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.luke.models.LukeException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.PointRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.SortedSetSortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SearchImplTest extends LuceneTestCase {

  private IndexReader reader;
  private Directory dir;
  private Path indexDir;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    createIndex();
    dir = newFSDirectory(indexDir);
    reader = DirectoryReader.open(dir);
  }

  private void createIndex() throws IOException {
    indexDir = createTempDir("testIndex");

    Directory dir = newFSDirectory(indexDir);
    RandomIndexWriter writer = new RandomIndexWriter(random(), dir, new StandardAnalyzer());

    for (int i = 0; i < 10; i++) {
      Document doc1 = new Document();
      doc1.add(newTextField("f1", "Apple Pie", Field.Store.YES));
      doc1.add(new SortedDocValuesField("f2", new BytesRef("a" + (i * 10 + 1))));
      doc1.add(new SortedSetDocValuesField("f3", new BytesRef("a" + (i * 10 + 1))));
      doc1.add(new NumericDocValuesField("f4", i * 10 + 1L));
      doc1.add(new FloatDocValuesField("f5", i * 10 + 1.0f));
      doc1.add(new DoubleDocValuesField("f6", i * 10 + 1.0));
      doc1.add(new SortedNumericDocValuesField("f7", i * 10 + 1L));
      doc1.add(new IntPoint("f8", i * 10 + 1));
      doc1.add(new LongPoint("f9", i * 10 + 1L));
      doc1.add(new FloatPoint("f10", i * 10 + 1.0f));
      doc1.add(new DoublePoint("f11", i * 10 + 1.0));
      writer.addDocument(doc1);

      Document doc2 = new Document();
      doc2.add(newTextField("f1", "Brownie", Field.Store.YES));
      doc2.add(new SortedDocValuesField("f2", new BytesRef("b" + (i * 10 + 2))));
      doc2.add(new SortedSetDocValuesField("f3", new BytesRef("b" + (i * 10 + 2))));
      doc2.add(new NumericDocValuesField("f4", i * 10 + 2L));
      doc2.add(new FloatDocValuesField("f5", i * 10 + 2.0f));
      doc2.add(new DoubleDocValuesField("f6", i * 10 + 2.0));
      doc2.add(new SortedNumericDocValuesField("f7", i * 10 + 2L));
      doc2.add(new IntPoint("f8", i * 10 + 2));
      doc2.add(new LongPoint("f9", i * 10 + 2L));
      doc2.add(new FloatPoint("f10", i * 10 + 2.0f));
      doc2.add(new DoublePoint("f11", i * 10 + 2.0));
      writer.addDocument(doc2);

      Document doc3 = new Document();
      doc3.add(newTextField("f1", "Chocolate Pie", Field.Store.YES));
      doc3.add(new SortedDocValuesField("f2", new BytesRef("c" + (i * 10 + 3))));
      doc3.add(new SortedSetDocValuesField("f3", new BytesRef("c" + (i * 10 + 3))));
      doc3.add(new NumericDocValuesField("f4", i * 10 + 3L));
      doc3.add(new FloatDocValuesField("f5", i * 10 + 3.0f));
      doc3.add(new DoubleDocValuesField("f6", i * 10 + 3.0));
      doc3.add(new SortedNumericDocValuesField("f7", i * 10 + 3L));
      doc3.add(new IntPoint("f8", i * 10 + 3));
      doc3.add(new LongPoint("f9", i * 10 + 3L));
      doc3.add(new FloatPoint("f10", i * 10 + 3.0f));
      doc3.add(new DoublePoint("f11", i * 10 + 3.0));
      writer.addDocument(doc3);

      Document doc4 = new Document();
      doc4.add(newTextField("f1", "Doughnut", Field.Store.YES));
      doc4.add(new SortedDocValuesField("f2", new BytesRef("d" + (i * 10 + 4))));
      doc4.add(new SortedSetDocValuesField("f3", new BytesRef("d" + (i * 10 + 4))));
      doc4.add(new NumericDocValuesField("f4", i * 10 + 4L));
      doc4.add(new FloatDocValuesField("f5", i * 10 + 4.0f));
      doc4.add(new DoubleDocValuesField("f6", i * 10 + 4.0));
      doc4.add(new SortedNumericDocValuesField("f7", i * 10 + 4L));
      doc4.add(new IntPoint("f8", i * 10 + 4));
      doc4.add(new LongPoint("f9", i * 10 + 4L));
      doc4.add(new FloatPoint("f10", i * 10 + 4.0f));
      doc4.add(new DoublePoint("f11", i * 10 + 4.0));
      writer.addDocument(doc4);

      Document doc5 = new Document();
      doc5.add(newTextField("f1", "Eclair", Field.Store.YES));
      doc5.add(new SortedDocValuesField("f2", new BytesRef("e" + (i * 10 + 5))));
      doc5.add(new SortedSetDocValuesField("f3", new BytesRef("e" + (i * 10 + 5))));
      doc5.add(new NumericDocValuesField("f4", i * 10 + 5L));
      doc5.add(new FloatDocValuesField("f5", i * 10 + 5.0f));
      doc5.add(new DoubleDocValuesField("f6", i * 10 + 5.0));
      doc5.add(new SortedNumericDocValuesField("f7", i * 10 + 5L));
      doc5.add(new IntPoint("f8", i * 10 + 5));
      doc5.add(new LongPoint("f9", i * 10 + 5L));
      doc5.add(new FloatPoint("f10", i * 10 + 5.0f));
      doc5.add(new DoublePoint("f11", i * 10 + 5.0));
      writer.addDocument(doc5);
    }
    writer.commit();
    writer.close();
    dir.close();
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    reader.close();
    dir.close();
  }

  @Test
  public void testGetSortableFieldNames() throws Exception {
    SearchImpl search = new SearchImpl();
    search.reset(reader);
    assertArrayEquals(new String[]{"f2", "f3", "f4", "f5", "f6", "f7"},
        search.getSortableFieldNames().toArray());
  }

  @Test
  public void testGetSearchableFieldNames() throws Exception {
    SearchImpl search = new SearchImpl();
    search.reset(reader);
    assertArrayEquals(new String[]{"f1"},
        search.getSearchableFieldNames().toArray());
  }

  @Test
  public void testGetRangeSearchableFieldNames() throws Exception {
    SearchImpl search = new SearchImpl();
    search.reset(reader);
    assertArrayEquals(new String[]{"f8", "f9", "f10", "f11"}, search.getRangeSearchableFieldNames().toArray());
  }

  @Test
  public void testParseClassic() throws Exception {
    SearchImpl search = new SearchImpl();
    search.reset(reader);
    QueryParserConfig config = new QueryParserConfig();
    config.setAllowLeadingWildcard(true);
    config.setDefaultOperator(QueryParserConfig.Operator.AND);
    config.setFuzzyMinSim(1.0f);
    Query q = search.parseQuery("app~ f2:*ie", "f1", new StandardAnalyzer(),
        config, false);
    assertEquals("+f1:app~1 +f2:*ie", q.toString());
  }

  @Test
  public void testParsePointRange() throws Exception {
    SearchImpl search = new SearchImpl();
    search.reset(reader);
    QueryParserConfig config = new QueryParserConfig();
    config.setUseClassicParser(false);
    Map<String, Class<? extends Number>> types = new HashMap<>();
    types.put("f8", Integer.class);
    config.setTypeMap(types);
    Query q = search.parseQuery("f8:[10 TO 20]", "f1", new StandardAnalyzer(),
        config, false);
    assertEquals("f8:[10 TO 20]", q.toString());
    assertTrue(q instanceof PointRangeQuery);
  }

  @Test
  public void testGuessSortTypes() throws Exception {
    SearchImpl search = new SearchImpl();
    search.reset(reader);

    assertTrue(search.guessSortTypes("f1").isEmpty());

    assertArrayEquals(
        new SortField[]{
            new SortField("f2", SortField.Type.STRING),
            new SortField("f2", SortField.Type.STRING_VAL)},
        search.guessSortTypes("f2").toArray());

    assertArrayEquals(
        new SortField[]{new SortedSetSortField("f3", false)},
        search.guessSortTypes("f3").toArray());

    assertArrayEquals(
        new SortField[]{
            new SortField("f4", SortField.Type.INT),
            new SortField("f4", SortField.Type.LONG),
            new SortField("f4", SortField.Type.FLOAT),
            new SortField("f4", SortField.Type.DOUBLE)},
        search.guessSortTypes("f4").toArray());

    assertArrayEquals(
        new SortField[]{
            new SortField("f5", SortField.Type.INT),
            new SortField("f5", SortField.Type.LONG),
            new SortField("f5", SortField.Type.FLOAT),
            new SortField("f5", SortField.Type.DOUBLE)},
        search.guessSortTypes("f5").toArray());

    assertArrayEquals(
        new SortField[]{
            new SortField("f6", SortField.Type.INT),
            new SortField("f6", SortField.Type.LONG),
            new SortField("f6", SortField.Type.FLOAT),
            new SortField("f6", SortField.Type.DOUBLE)},
        search.guessSortTypes("f6").toArray());

    assertArrayEquals(
        new SortField[]{
            new SortedNumericSortField("f7", SortField.Type.INT),
            new SortedNumericSortField("f7", SortField.Type.LONG),
            new SortedNumericSortField("f7", SortField.Type.FLOAT),
            new SortedNumericSortField("f7", SortField.Type.DOUBLE)},
        search.guessSortTypes("f7").toArray());
  }

  @Test(expected = LukeException.class)
  public void testGuessSortTypesNoSuchField() throws Exception {
    SearchImpl search = new SearchImpl();
    search.reset(reader);
    search.guessSortTypes("unknown");
  }

  @Test
  public void testGetSortType() throws Exception {
    SearchImpl search = new SearchImpl();
    search.reset(reader);

    assertFalse(search.getSortType("f1", "STRING", false).isPresent());

    assertEquals(new SortField("f2", SortField.Type.STRING, false),
        search.getSortType("f2", "STRING", false).get());
    assertFalse(search.getSortType("f2", "INT", false).isPresent());

    assertEquals(new SortedSetSortField("f3", false),
        search.getSortType("f3", "CUSTOM", false).get());

    assertEquals(new SortField("f4", SortField.Type.LONG, false),
        search.getSortType("f4", "LONG", false).get());
    assertFalse(search.getSortType("f4", "STRING", false).isPresent());

    assertEquals(new SortField("f5", SortField.Type.FLOAT, false),
        search.getSortType("f5", "FLOAT", false).get());
    assertFalse(search.getSortType("f5", "STRING", false).isPresent());

    assertEquals(new SortField("f6", SortField.Type.DOUBLE, false),
        search.getSortType("f6", "DOUBLE", false).get());
    assertFalse(search.getSortType("f6", "STRING", false).isPresent());

    assertEquals(new SortedNumericSortField("f7", SortField.Type.LONG, false),
        search.getSortType("f7", "LONG", false).get());
    assertFalse(search.getSortType("f7", "STRING", false).isPresent());
  }

  @Test(expected = LukeException.class)
  public void testGetSortTypeNoSuchField() throws Exception {
    SearchImpl search = new SearchImpl();
    search.reset(reader);

    search.getSortType("unknown", "STRING", false);
  }

  @Test
  public void testSearch() throws Exception {
    SearchImpl search = new SearchImpl();
    search.reset(reader);
    Query query = new QueryParser("f1", new StandardAnalyzer()).parse("apple");
    Optional<SearchResults> opt = search.search(query, new SimilarityConfig(), null, 10);
    assertTrue(opt.isPresent());

    SearchResults res = opt.get();
    assertEquals(10, res.getTotalHits());
    assertEquals(10, res.size());
    assertEquals(0, res.getOffset());
    printResults(res);
  }

  @Test
  public void testSearchWithSort() throws Exception {
    SearchImpl search = new SearchImpl();
    search.reset(reader);
    Query query = new QueryParser("f1", new StandardAnalyzer()).parse("apple");
    Sort sort = new Sort(new SortField("f2", SortField.Type.STRING, true));
    Optional<SearchResults> opt = search.search(query, new SimilarityConfig(), sort, null, 10);
    assertTrue(opt.isPresent());

    SearchResults res = opt.get();
    assertEquals(10, res.getTotalHits());
    assertEquals(10, res.size());
    assertEquals(0, res.getOffset());
    printResults(res);
  }

  @Test
  public void testNextPage() throws Exception {
    SearchImpl search = new SearchImpl();
    search.reset(reader);
    Query query = new QueryParser("f1", new StandardAnalyzer()).parse("pie");
    search.search(query, new SimilarityConfig(), null, 10);
    Optional<SearchResults> opt = search.nextPage();
    assertTrue(opt.isPresent());

    SearchResults res = opt.get();
    assertEquals(20, res.getTotalHits());
    assertEquals(10, res.size());
    assertEquals(10, res.getOffset());
    printResults(res);
  }

  @Test(expected = LukeException.class)
  public void testNextPageSearchNotStarted() throws Exception {
    SearchImpl search = new SearchImpl();
    search.reset(reader);
    search.nextPage();
  }

  @Test(expected = LukeException.class)
  public void testNextPageNoMoreResults() throws Exception {
    SearchImpl search = new SearchImpl();
    search.reset(reader);
    Query query = new QueryParser("f1", new StandardAnalyzer()).parse("pie");
    search.search(query, new SimilarityConfig(), null, 10);
    search.nextPage();
    search.nextPage();
  }

  @Test
  public void testPrevPage() throws Exception {
    SearchImpl search = new SearchImpl();
    search.reset(reader);
    Query query = new QueryParser("f1", new StandardAnalyzer()).parse("pie");
    search.search(query, new SimilarityConfig(), null, 10);
    search.nextPage();
    Optional<SearchResults> opt = search.prevPage();
    assertTrue(opt.isPresent());

    SearchResults res = opt.get();
    assertEquals(20, res.getTotalHits());
    assertEquals(10, res.size());
    assertEquals(0, res.getOffset());
    printResults(res);
  }

  @Test(expected = LukeException.class)
  public void testPrevPageSearchNotStarted() throws Exception {
    SearchImpl search = new SearchImpl();
    search.reset(reader);
    search.prevPage();
  }

  @Test(expected = LukeException.class)
  public void testPrevPageNoMoreResults() throws Exception {
    SearchImpl search = new SearchImpl();
    search.reset(reader);
    Query query = new QueryParser("f1", new StandardAnalyzer()).parse("pie");
    search.search(query, new SimilarityConfig(), null, 10);
    search.prevPage();
  }

  private void printResults(SearchResults res) {
    System.out.println("**search results**");
    System.out.println("total=" + res.getTotalHits());
    System.out.println("offset=" + res.getOffset());
    System.out.println("hits:");
    for (SearchResults.Doc doc : res.getHits()) {
      System.out.print(" id=" + doc.getDocId());
      System.out.print(" score=" + doc.getScore());
      System.out.print(" values=" + String.join(",",
          doc.getFieldValues().entrySet().stream().map(e ->
              e.getKey() + ":" + String.join(";", Arrays.asList(e.getValue())))
              .collect(Collectors.toList())));
      System.out.println();
    }
  }
}
