package edu.cmu.lti.f14.hw3.hw3_ajng.annotators;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
  Set<String> stopWords;

  /**
   * Initialize stop words
   */
  @Override
  public void initialize(org.apache.uima.UimaContext aContext)
          throws org.apache.uima.resource.ResourceInitializationException {
    stopWords = new HashSet<String>();
    try {
      BufferedReader br = new BufferedReader(new FileReader(new File("src/main/resources/stopwords.txt")));
      String line;
      while ((line = br.readLine()) != null) {
        stopWords.add(line.toLowerCase());
      }
    } catch (FileNotFoundException e) {

      e.printStackTrace();
      System.exit(12);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(13);
    }
  };

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
   * A more advanced tokenizer that tokenizes based on non-word characters
   */
  List<String> tokenize1(String doc) {
    List<String> res = new ArrayList<String>();

    for (String s : doc.split("[^\\w]+")) {
      s = s.toLowerCase();
      if (!stopWords.contains(s))
        res.add(s);
    }
    return res;
  }

  /**
   * Create a vector of words in the sentence
   * 
   * @param jcas
   * @param doc
   */

  private void createTermFreqVector(JCas jcas, Document doc) {
    String docText = doc.getText();

    Map<String, Integer> frequencies = new HashMap<String, Integer>();
    for (String word : tokenize0(docText)) {
      System.err.println(word);
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
