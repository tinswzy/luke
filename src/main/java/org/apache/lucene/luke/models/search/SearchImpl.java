package org.apache.lucene.luke.models.search;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.luke.models.BaseModel;
import org.apache.lucene.luke.models.LukeException;
import org.apache.lucene.luke.util.IndexUtils;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.queryparser.flexible.standard.config.StandardQueryConfigHandler;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.SortedSetSortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SearchImpl extends BaseModel implements Search {

  private static final Logger logger = LoggerFactory.getLogger(SearchImpl.class);

  private static final int DEFAULT_PAGE_SIZE = 10;

  private IndexSearcher searcher;

  private int pageSize;

  private int currentPage;

  private long totalHits;

  private ScoreDoc[] docs;

  private Query query;

  private Sort sort;

  private Set<String> fieldsToLoad;

  @Inject
  public SearchImpl() {
    pageSize = DEFAULT_PAGE_SIZE;
    currentPage = -1;
    totalHits = -1;
    docs = new ScoreDoc[0];
  }

  @Override
  public void reset(IndexReader reader) throws LukeException {
    super.reset(reader);
    this.searcher = new IndexSearcher(reader);
  }

  @Override
  public Collection<String> getSortableFieldNames() {
    Collection<String> fields = IndexUtils.getFieldNames(reader);
    return fields.stream()
        .map(f -> IndexUtils.getFieldInfo(reader, f))
        .filter(info -> !info.getDocValuesType().equals(DocValuesType.NONE))
        .map(info -> info.name)
        .collect(Collectors.toList());
  }

  @Override
  public Collection<String> getSearchableFieldNames() {
    Collection<String> fields = IndexUtils.getFieldNames(reader);
    return fields.stream()
        .map(f -> IndexUtils.getFieldInfo(reader, f))
        .filter(info -> !info.getIndexOptions().equals(IndexOptions.NONE))
        .map(info -> info.name)
        .collect(Collectors.toList());
  }

  @Override
  public Collection<String> getRangeSearchableFieldNames() {
    Collection<String> fields = IndexUtils.getFieldNames(reader);
    return fields.stream()
        .map(f -> IndexUtils.getFieldInfo(reader, f))
        .filter(info -> info.getPointDimensionCount() > 0)
        .map(info -> info.name)
        .collect(Collectors.toSet());
  }

  @Override
  public Query getCurrentQuery() {
    return this.query;
  }

  @Override
  public Query parseQuery(@Nonnull String expression, @Nonnull String defField, @Nonnull Analyzer analyzer,
                          @Nonnull QueryParserConfig config, boolean rewrite) throws LukeException {
    //logger.debug("expression=" + expression);
    //logger.debug("defField=" + defField);
    //logger.debug("parser config=" + config.toString());
    //logger.debug("rewrite=" + rewrite);

    Query query = config.isUseClassicParser() ?
        parseByClassicParser(expression, defField, analyzer, config) :
        parseByStandardParser(expression, defField, analyzer, config);

    if (rewrite) {
      try {
        query = query.rewrite(reader);
      } catch (IOException e) {
        String msg = "Failed to rewrite query: " + query.toString();
        logger.error(msg, e);
        throw new LukeException(msg, e);
      }
    }

    return query;
  }

  private Query parseByClassicParser(@Nonnull String expression, @Nonnull String defField, @Nonnull Analyzer analyzer,
                                     @Nonnull QueryParserConfig config) throws LukeException {
    QueryParser parser = new QueryParser(defField, analyzer);

    switch (config.getDefaultOperator()) {
      case OR:
        parser.setDefaultOperator(QueryParser.Operator.OR);
        break;
      case AND:
        parser.setDefaultOperator(QueryParser.Operator.AND);
        break;
    }

    parser.setAutoGenerateMultiTermSynonymsPhraseQuery(config.isAutoGenerateMultiTermSynonymsPhraseQuery());
    parser.setAutoGeneratePhraseQueries(config.isAutoGeneratePhraseQueries());
    parser.setEnablePositionIncrements(config.isEnablePositionIncrements());
    parser.setAllowLeadingWildcard(config.isAllowLeadingWildcard());
    parser.setSplitOnWhitespace(config.isSplitOnWhitespace());
    parser.setDateResolution(config.getDateResolution());
    parser.setFuzzyMinSim(config.getFuzzyMinSim());
    parser.setFuzzyPrefixLength(config.getFuzzyPrefixLength());
    parser.setLocale(config.getLocale());
    parser.setTimeZone(config.getTimeZone());
    parser.setPhraseSlop(config.getPhraseSlop());

    try {
      return parser.parse(expression);
    } catch (ParseException e) {
      String msg = "Failed to parser query expression: " + expression;
      logger.error(msg, e);
      throw new LukeException(msg, e);
    }

  }

  private Query parseByStandardParser(@Nonnull String expression, @Nonnull String defField, @Nonnull Analyzer analyzer,
                                      @Nonnull QueryParserConfig config) throws LukeException {
    StandardQueryParser parser = new StandardQueryParser(analyzer);

    switch (config.getDefaultOperator()) {
      case OR:
        parser.setDefaultOperator(StandardQueryConfigHandler.Operator.OR);
        break;
      case AND:
        parser.setDefaultOperator(StandardQueryConfigHandler.Operator.AND);
        break;
    }

    parser.setEnablePositionIncrements(config.isEnablePositionIncrements());
    parser.setAllowLeadingWildcard(config.isAllowLeadingWildcard());
    parser.setDateResolution(config.getDateResolution());
    parser.setFuzzyMinSim(config.getFuzzyMinSim());
    parser.setFuzzyPrefixLength(config.getFuzzyPrefixLength());
    parser.setLocale(config.getLocale());
    parser.setTimeZone(config.getTimeZone());
    parser.setPhraseSlop(config.getPhraseSlop());

    if (config.getTypeMap() != null) {
      Map<String, PointsConfig> pointsConfigMap = new HashMap<>();
      for (Map.Entry<String, Class<? extends Number>> entry : config.getTypeMap().entrySet()) {
        String field = entry.getKey();
        Class<? extends Number> type = entry.getValue();
        PointsConfig pc;
        if (type == Integer.class || type == Long.class) {
          pc = new PointsConfig(NumberFormat.getIntegerInstance(Locale.ROOT), type);
        } else if (type == Float.class || type == Double.class) {
          pc = new PointsConfig(NumberFormat.getNumberInstance(Locale.ROOT), type);
        } else {
          logger.warn(String.format("Ignored invalid number type: %s.", type.getName()));
          continue;
        }
        pointsConfigMap.put(field, pc);
      }
      parser.setPointsConfigMap(pointsConfigMap);
    }

    try {
      return parser.parse(expression, defField);
    } catch (QueryNodeException e) {
      String msg = "Failed to parser query expression: " + expression;
      logger.error(msg, e);
      throw new LukeException(msg, e);
    }

  }

  @Override
  public Query mltQuery(int docNum, MLTConfig mltConfig, Analyzer analyzer) throws LukeException {
    MoreLikeThis mlt = new MoreLikeThis(reader);
    mlt.setAnalyzer(analyzer);
    mlt.setFieldNames(mltConfig.getFieldNames());
    mlt.setMinDocFreq(mltConfig.getMinDocFreq());
    mlt.setMaxDocFreq(mltConfig.getMaxDocFreq());
    mlt.setMinTermFreq(mltConfig.getMinTermFreq());
    try {
      return mlt.like(docNum);
    } catch (IOException e) {
      throw new LukeException("Failed to create MLT query for doc: " + docNum);
    }
  }

  @Override
  public Optional<SearchResults> search(
      @Nonnull Query query, @Nonnull SimilarityConfig simConfig, @Nullable Set<String> fieldsToLoad, int pageSize)
      throws LukeException {
    return search(query, simConfig, null, fieldsToLoad, pageSize);
  }

  @Override
  public Optional<SearchResults> search(
      @Nonnull Query query, @Nonnull SimilarityConfig simConfig, @Nullable Sort sort, @Nullable Set<String> fieldsToLoad, int pageSize)
      throws LukeException {
    if (pageSize < 0) {
      throw new LukeException(new IllegalArgumentException("Negative integer is not acceptable for page size."));
    }

    this.docs = new ScoreDoc[0];
    this.currentPage = 0;
    this.pageSize = pageSize;
    this.query = query;
    this.sort = sort;
    this.fieldsToLoad = fieldsToLoad;

    //logger.debug("query=" + query);
    //logger.debug("pageSize=" + pageSize);
    //logger.debug("similarity config=" + simConfig.toString());
    //logger.debug("sort=" + (sort == null ? "(null)" : sort.toString()));
    //logger.debug("fields=" + fieldsToLoad);

    searcher.setSimilarity(createSimilarity(simConfig));

    try {
      return Optional.of(search());
    } catch (IOException e) {
      logger.error("Search Failed.", e);
    }
    return Optional.empty();
  }

  @Override
  public Optional<SearchResults> nextPage() throws LukeException {
    if (currentPage < 0 || query == null) {
      throw new LukeException(new IllegalStateException("Search session not started."));
    }
    currentPage += 1;
    if (totalHits == 0 || currentPage * pageSize >= totalHits) {
      throw new LukeException(new IllegalStateException("No more next search results are available."));
    }
    try {
      if (currentPage * pageSize < docs.length) {
        int from = currentPage * pageSize;
        int to = Math.min(from + pageSize, docs.length);
        ScoreDoc[] part = Arrays.copyOfRange(docs, from, to);
        return Optional.of(SearchResults.of(totalHits, part, from, searcher, fieldsToLoad));
      } else {
        return Optional.of(search());
      }
    } catch (IOException e) {
      logger.error("Search failed.", e);
    }
    return Optional.empty();
  }

  private SearchResults search() throws IOException {
    ScoreDoc after = docs.length == 0 ? null : docs[docs.length - 1];
    TopDocs topDocs = sort == null ?
        searcher.searchAfter(after, query, pageSize) :
        searcher.searchAfter(after, query, pageSize, sort);
    ScoreDoc[] newDocs = new ScoreDoc[docs.length + topDocs.scoreDocs.length];
    System.arraycopy(docs, 0, newDocs, 0, docs.length);
    System.arraycopy(topDocs.scoreDocs, 0, newDocs, docs.length, topDocs.scoreDocs.length);

    this.totalHits = topDocs.totalHits;
    this.docs = newDocs;

    return SearchResults.of(topDocs.totalHits, topDocs.scoreDocs, currentPage * pageSize, searcher, fieldsToLoad);
  }

  @Override
  public Optional<SearchResults> prevPage() throws LukeException {
    if (currentPage < 0 || query == null) {
      throw new LukeException(new IllegalStateException("Search session not started."));
    }
    currentPage -= 1;
    if (currentPage < 0) {
      throw new LukeException(new IllegalStateException("No more previous search results are available."));
    }
    try {
      int from = currentPage * pageSize;
      int to = Math.min(from + pageSize, docs.length);
      ScoreDoc[] part = Arrays.copyOfRange(docs, from, to);
      return Optional.of(SearchResults.of(totalHits, part, from, searcher, fieldsToLoad));
    } catch (IOException e) {
      logger.error("Search failed.", e);
    }
    return Optional.empty();
  }

  private Similarity createSimilarity(@Nonnull SimilarityConfig config) {
    Similarity similarity;
    if (config.isUseClassicSimilarity()) {
      ClassicSimilarity tfidf = new ClassicSimilarity();
      tfidf.setDiscountOverlaps(config.isDiscountOverlaps());
      similarity = tfidf;
    } else {
      BM25Similarity bm25 = new BM25Similarity(config.getK1(), config.getB());
      bm25.setDiscountOverlaps(config.isDiscountOverlaps());
      similarity = bm25;
    }
    return similarity;
  }

  @Override
  public List<SortField> guessSortTypes(String name) throws LukeException {
    FieldInfo finfo = IndexUtils.getFieldInfo(reader, name);
    if (finfo == null) {
      throw new LukeException("No such field: " + name, new IllegalArgumentException());
    }
    DocValuesType dvType = finfo.getDocValuesType();
    switch (dvType) {
      case NONE:
        return Collections.emptyList();
      case NUMERIC:
        return Lists.newArrayList(
            new SortField(name, SortField.Type.INT),
            new SortField(name, SortField.Type.LONG),
            new SortField(name, SortField.Type.FLOAT),
            new SortField(name, SortField.Type.DOUBLE));
      case SORTED_NUMERIC:
        return Lists.newArrayList(
            new SortedNumericSortField(name, SortField.Type.INT),
            new SortedNumericSortField(name, SortField.Type.LONG),
            new SortedNumericSortField(name, SortField.Type.FLOAT),
            new SortedNumericSortField(name, SortField.Type.DOUBLE)
        );
      case SORTED:
        return Lists.newArrayList(
            new SortField(name, SortField.Type.STRING),
            new SortField(name, SortField.Type.STRING_VAL)
        );
      case SORTED_SET:
        return Collections.singletonList(new SortedSetSortField(name, false));
      default:
        return Collections.singletonList(new SortField(name, SortField.Type.DOC));
    }
  }

  @Override
  public Optional<SortField> getSortType(@Nonnull String name, @Nonnull String type, boolean reverse)
      throws LukeException {
    List<SortField> candidates = guessSortTypes(name);
    if (candidates.isEmpty()) {
      logger.warn(String.format("No available sort types for: %s", name));
      return Optional.empty();
    }
    for (SortField sf : candidates) {
      if (sf instanceof SortedSetSortField) {
        return Optional.of(new SortedSetSortField(sf.getField(), reverse));
      } else if (sf instanceof SortedNumericSortField) {
        SortField.Type sfType = ((SortedNumericSortField) sf).getNumericType();
        if (sfType.name().equals(type)) {
          return Optional.of(new SortedNumericSortField(sf.getField(), sfType, reverse));
        }
      } else {
        SortField.Type sfType = sf.getType();
        if (sfType.name().equals(type)) {
          return Optional.of(new SortField(sf.getField(), sfType, reverse));
        }
      }
    }
    return Optional.empty();
  }

  @Override
  public Explanation explain(Query query, int doc) throws LukeException {
    try {
      return searcher.explain(query, doc);
    } catch (IOException e) {
      String msg = String.format("Failed to create explanation for doc: %d against query: \"%s\"", doc, query.toString());
      logger.error(msg, e);
      throw new LukeException(msg, e);
    }
  }
}
