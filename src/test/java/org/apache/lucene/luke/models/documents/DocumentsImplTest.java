package org.apache.lucene.luke.models.documents;

import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.Term;
import org.apache.lucene.luke.models.LukeException;
import org.apache.lucene.luke.util.IndexUtils;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.util.NumericUtils;
import org.junit.Test;

import java.util.List;


public class DocumentsImplTest extends DocumentsTestBase {

  @Test
  public void testGetMaxDoc() throws LukeException {
    DocumentsImpl documents = new DocumentsImpl();
    documents.reset(reader);
    assertEquals(5, documents.getMaxDoc());
  }

  @Test
  public void testIsLive() throws Exception {
    DocumentsImpl documents = new DocumentsImpl();
    documents.reset(reader);
    assertTrue(documents.isLive(0));
  }

  @Test
  public void testGetDocumentFields() throws Exception {
    DocumentsImpl documents = new DocumentsImpl();
    documents.reset(reader);
    List<DocumentField> fields = documents.getDocumentFields(0).orElseThrow(IllegalStateException::new);
    assertEquals(5, fields.size());

    DocumentField f1 = fields.get(0);
    assertEquals("title", f1.getName());
    assertEquals(IndexOptions.DOCS_AND_FREQS, f1.getIdxOptions());
    assertFalse(f1.hasTermVectors());
    assertFalse(f1.hasPayloads());
    assertFalse(f1.hasNorms());
    assertEquals(0, f1.getNorm());
    assertTrue(f1.isStored());
    assertEquals("Pride and Prejudice", f1.getStringValue());
    assertNull(f1.getBinaryValue());
    assertNull(f1.getNumericValue());
    assertEquals(DocValuesType.NONE, f1.getDvType());
    assertEquals(0, f1.getPointDimensionCount());
    assertEquals(0, f1.getPointNumBytes());

    DocumentField f2 = fields.get(1);
    assertEquals("author", f2.getName());
    assertEquals(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS, f2.getIdxOptions());
    assertFalse(f2.hasTermVectors());
    assertFalse(f2.hasPayloads());
    assertTrue(f2.hasNorms());
    assertTrue(f2.getNorm() > 0);
    assertTrue(f2.isStored());
    assertEquals("Jane Austen", f2.getStringValue());
    assertNull(f2.getBinaryValue());
    assertNull(f2.getNumericValue());
    assertEquals(DocValuesType.NONE, f2.getDvType());
    assertEquals(0, f2.getPointDimensionCount());
    assertEquals(0, f2.getPointNumBytes());

    DocumentField f3 = fields.get(2);
    assertEquals("text", f3.getName());
    assertEquals(IndexOptions.DOCS_AND_FREQS, f3.getIdxOptions());
    assertTrue(f3.hasTermVectors());
    assertFalse(f3.hasPayloads());
    assertTrue(f3.hasNorms());
    assertTrue(f3.getNorm() > 0);
    assertFalse(f3.isStored());
    assertNull(f3.getStringValue());
    assertNull(f3.getBinaryValue());
    assertNull(f3.getNumericValue());
    assertEquals(DocValuesType.NONE, f3.getDvType());
    assertEquals(0, f3.getPointDimensionCount());
    assertEquals(0, f3.getPointNumBytes());

    DocumentField f4 = fields.get(3);
    assertEquals("subject", f4.getName());
    assertEquals(IndexOptions.NONE, f4.getIdxOptions());
    assertFalse(f4.hasTermVectors());
    assertFalse(f4.hasPayloads());
    assertFalse(f4.hasNorms());
    assertEquals(0, f4.getNorm());
    assertFalse(f4.isStored());
    assertNull(f4.getStringValue());
    assertNull(f4.getBinaryValue());
    assertNull(f4.getNumericValue());
    assertEquals(DocValuesType.SORTED_SET, f4.getDvType());
    assertEquals(0, f4.getPointDimensionCount());
    assertEquals(0, f4.getPointNumBytes());

    DocumentField f5 = fields.get(4);
    assertEquals("downloads", f5.getName());
    assertEquals(IndexOptions.NONE, f5.getIdxOptions());
    assertFalse(f5.hasTermVectors());
    assertFalse(f5.hasPayloads());
    assertFalse(f5.hasNorms());
    assertEquals(0, f5.getNorm());
    assertTrue(f5.isStored());
    assertNull(f5.getStringValue());
    assertEquals(28533, NumericUtils.sortableBytesToInt(f5.getBinaryValue().bytes, 0));
    assertNull(f5.getNumericValue());
  }

  @Test
  public void testFirstTerm() throws Exception {
    DocumentsImpl documents = new DocumentsImpl();
    documents.reset(reader);
    Term term = documents.firstTerm("title").orElseThrow(IllegalStateException::new);
    assertEquals("title", documents.getCurrentField());
    assertEquals("adventures", term.text());
  }

  @Test
  public void testFirstTerm_notAvailable() throws Exception {
    DocumentsImpl documents = new DocumentsImpl();
    documents.reset(reader);
    assertFalse(documents.firstTerm("subject").isPresent());
  }

  @Test
  public void testNextTerm() throws Exception {
    DocumentsImpl documents = new DocumentsImpl();
    documents.reset(reader);
    documents.firstTerm("title").orElseThrow(IllegalStateException::new);
    Term term = documents.nextTerm().orElseThrow(IllegalStateException::new);
    assertEquals("alice's", term.text());

    while (documents.nextTerm().isPresent()) {
      Integer freq = documents.getDocFreq().orElseThrow(IllegalStateException::new);
    }
  }

  @Test
  public void testNextTerm_unPositioned() throws Exception {
    DocumentsImpl documents = new DocumentsImpl();
    documents.reset(reader);
    assertFalse(documents.nextTerm().isPresent());
  }

  @Test
  public void testSeekTerm() throws Exception {
    DocumentsImpl documents = new DocumentsImpl();
    documents.reset(reader);
    documents.firstTerm("title").orElseThrow(IllegalStateException::new);
    Term term = documents.seekTerm("pri").orElseThrow(IllegalStateException::new);
    assertEquals("pride", term.text());

    assertFalse(documents.seekTerm("x").isPresent());
  }

  @Test
  public void testSeekTerm_unPositioned() throws Exception {
    DocumentsImpl documents = new DocumentsImpl();
    documents.reset(reader);
    assertFalse(documents.seekTerm("a").isPresent());
  }

  @Test
  public void testFirstTermDoc() throws Exception {
    DocumentsImpl documents = new DocumentsImpl();
    documents.reset(reader);
    documents.firstTerm("title").orElseThrow(IllegalStateException::new);
    Term term = documents.seekTerm("adv").orElseThrow(IllegalStateException::new);
    assertEquals("adventures", term.text());
    int docid = documents.firstTermDoc().orElseThrow(IllegalStateException::new);
    assertEquals(1, docid);
  }

  @Test
  public void testFirstTermDoc_unPositioned() throws Exception {
    DocumentsImpl documents = new DocumentsImpl();
    documents.reset(reader);
    assertFalse(documents.firstTermDoc().isPresent());
  }

  @Test
  public void testNextTermDoc() throws Exception {
    DocumentsImpl documents = new DocumentsImpl();
    documents.reset(reader);
    Term term = documents.firstTerm("title").orElseThrow(IllegalStateException::new);
    term = documents.seekTerm("adv").orElseThrow(IllegalStateException::new);
    assertEquals("adventures", term.text());
    int docid = documents.firstTermDoc().orElseThrow(IllegalStateException::new);
    docid = documents.nextTermDoc().orElseThrow(IllegalStateException::new);
    assertEquals(4, docid);

    assertFalse(documents.nextTermDoc().isPresent());
  }

  @Test
  public void testNextTermDoc_unPositioned() throws Exception {
    DocumentsImpl documents = new DocumentsImpl();
    documents.reset(reader);
    Term term = documents.firstTerm("title").orElseThrow(IllegalStateException::new);
    assertFalse(documents.nextTermDoc().isPresent());
  }

  @Test
  public void testTermPositions() throws Exception {
    DocumentsImpl documents = new DocumentsImpl();
    documents.reset(reader);
    Term term = documents.firstTerm("author").orElseThrow(IllegalStateException::new);
    term = documents.seekTerm("carroll").orElseThrow(IllegalStateException::new);
    int docid = documents.firstTermDoc().orElseThrow(IllegalStateException::new);
    List<TermPosting> postings = documents.getTermPositions();
    assertEquals(1, postings.size());
    assertEquals(1, postings.get(0).getPosition());
    assertEquals(6, postings.get(0).getStartOffset());
    assertEquals(13, postings.get(0).getEndOffset());
  }

  @Test
  public void testTermPositions_unPositioned() throws Exception {
    DocumentsImpl documents = new DocumentsImpl();
    documents.reset(reader);
    Term term = documents.firstTerm("author").orElseThrow(IllegalStateException::new);
    assertEquals(0, documents.getTermPositions().size());
  }

  @Test
  public void testTermPositions_noPositions() throws Exception {
    DocumentsImpl documents = new DocumentsImpl();
    documents.reset(reader);
    Term term = documents.firstTerm("title").orElseThrow(IllegalStateException::new);
    int docid = documents.firstTermDoc().orElseThrow(IllegalStateException::new);
    assertEquals(0, documents.getTermPositions().size());
  }

  @Test(expected = AlreadyClosedException.class)
  public void testClose() throws Exception {
    DocumentsImpl documents = new DocumentsImpl();
    documents.reset(reader);
    reader.close();
    IndexUtils.getFieldNames(reader);
  }
}
