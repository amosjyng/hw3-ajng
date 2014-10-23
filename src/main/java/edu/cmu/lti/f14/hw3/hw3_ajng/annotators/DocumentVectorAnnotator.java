package edu.cmu.lti.f14.hw3.hw3_ajng.annotators;

import java.util.*;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.lti.f14.hw3.hw3_ajng.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_ajng.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_ajng.utils.Utils;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {

    FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
    if (iter.isValid()) {
      iter.moveToNext();
      Document doc = (Document) iter.get();
      createTermFreqVector(jcas, doc);
    }

  }

  /**
   * A basic white-space tokenizer, it deliberately does not split on punctuation!
   *
   * @param doc
   *          input text
   * @return a list of tokens.
   */

  List<String> tokenize0(String doc) {
    List<String> res = new ArrayList<String>();

    for (String s : doc.split("\\s+"))
      res.add(s);
    return res;
  }

  /**
   * Create a vector of words in the sentence
   * @param jcas
   * @param doc
   */

  private void createTermFreqVector(JCas jcas, Document doc) {
    String docText = doc.getText();

    Map<String, Integer> frequencies = new HashMap<String, Integer>();
    for (String word : tokenize0(docText)) {
      if (frequencies.containsKey(word)) {
        frequencies.put(word, frequencies.get(word) + 1);
      } else {
        frequencies.put(word, 1);
      }
    }
    List<Token> tokens = new ArrayList<Token>();
    for (Map.Entry<String, Integer> e : frequencies.entrySet()) {
      Token token = new Token(jcas);
      token.setText(e.getKey());
      token.setFrequency(e.getValue());
      tokens.add(token);
    }
    doc.setTokenList(Utils.fromCollectionToFSList(jcas, tokens));
  }

}
