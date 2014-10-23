package edu.cmu.lti.f14.hw3.hw3_ajng.casconsumers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f14.hw3.hw3_ajng.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_ajng.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_ajng.utils.Utils;

public class RetrievalEvaluator extends CasConsumer_ImplBase {

  /** query id number **/
  public ArrayList<Integer> qIdList;

  /** query and text relevant values **/
  public ArrayList<Integer> relList;

  /** doc vectors **/
  public List<Map<String, Integer>> docVectors;

  /** doc text **/
  public List<String> sentences;

  public void initialize() throws ResourceInitializationException {

    qIdList = new ArrayList<Integer>();

    relList = new ArrayList<Integer>();

    docVectors = new ArrayList<Map<String, Integer>>();

    sentences = new ArrayList<String>();
  }

  /**
   * TODO :: 1. construct the global word dictionary 2. keep the word frequency for each sentence
   */
  @Override
  public void processCas(CAS aCas) throws ResourceProcessException {

    JCas jcas;
    try {
      jcas = aCas.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }

    FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();

    if (it.hasNext()) {
      Document doc = (Document) it.next();

      // Make sure that your previous annotators have populated this in CAS
      FSList fsTokenList = doc.getTokenList();
      // ArrayList<Token>tokenList=Utils.fromFSListToCollection(fsTokenList, Token.class);

      qIdList.add(doc.getQueryID());
      relList.add(doc.getRelevanceValue());
      docVectors
              .add(Utils.tokenListToVector(Utils.fromFSListToCollection(fsTokenList, Token.class)));
      sentences.add(doc.getText());
    }

  }

  /**
   * Get the query vector
   */
  public Map<String, Integer> findQueryVector(Integer qId) {
    for (int i = 0; i < qIdList.size(); i++) {
      if (qIdList.get(i) == qId && relList.get(i) == 99) {
        return docVectors.get(i);
      }
    }
    System.err.println("Question " + qId + " not found!");
    System.exit(3);
    return null;
  }

  /**
   * Find where the most relevant result is
   */
  public Integer findTopId(Integer qId, Map<String, Integer> queryVector) {
    double highestSimilarity = 0;
    int topId = -1;
    for (int i = 0; i < qIdList.size(); i++) {
      if (qIdList.get(i) == qId && relList.get(i) != 99) {
        double thisSimilarity = computeCosineSimilarity(queryVector, docVectors.get(i));
        if (thisSimilarity > highestSimilarity) {
          highestSimilarity = thisSimilarity;
          topId = i;
        }
      }
    }
    if (topId == -1) {
      System.err.println("couldn't find top for question " + qId);
      System.exit(5);
    }
    return topId;
  }

  /**
   * Find where the answer to the question is
   */
  public Integer findRelevantId(Integer qId) {
    for (int i = 0; i < qIdList.size(); i++) {
      if (qIdList.get(i) == qId && relList.get(i) == 1) {
        return i;
      }
    }
    System.err.println("Answer for question " + qId + " not found!");
    System.exit(2);
    return -1;
  }

  /**
   * Find the ranking of the correct answer among all answers for that question
   */
  public Integer findRelevantRank(Integer qId, Map<String, Integer> queryVector, double similarity) {
    int rank = 1;
    for (int i = 0; i < qIdList.size(); i++) {
      if (qIdList.get(i) == qId && relList.get(i) == 0
              && computeCosineSimilarity(queryVector, docVectors.get(i)) > similarity) {
        rank++;
      }
    }
    return rank;
  }

  /**
   * TODO 1. Compute Cosine Similarity and rank the retrieved sentences 2. Compute the MRR metric
   */
  @Override
  public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
          IOException {

    super.collectionProcessComplete(arg0);

    BufferedWriter bw = null;
    try {
      bw = new BufferedWriter(new FileWriter("report.txt"));
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }

    List<Integer> correctRankings = new ArrayList<Integer>();
    for (Integer qId : new HashSet<Integer>(qIdList)) {
      int answerIndex = findRelevantId(qId);
      Map<String, Integer> queryVector = findQueryVector(qId);
      double cosine_similarity = computeCosineSimilarity(queryVector, docVectors.get(answerIndex));
      int rank = findRelevantRank(qId, queryVector, cosine_similarity);
      bw.write(String.format("cosine=%.4f\trank=%d\tqid=%d\trel=%d\t%s\n", cosine_similarity, rank,
              qId, relList.get(answerIndex), sentences.get(answerIndex)));
      correctRankings.add(rank);
    }
    // compute the metric:: mean reciprocal rank
    double metric_mrr = compute_mrr(correctRankings);
    System.out.println(" (MRR) Mean Reciprocal Rank ::" + metric_mrr);
    bw.write("MRR=" + String.format("%.4f", metric_mrr));
    bw.close();
  }

  /**
   * Return the magnitude of a vector
   */
  private double computeVectorMagnitude(Map<String, Integer> vector) {
    double magnitude = 0;
    for (Integer axisDistance : vector.values()) {
      magnitude = axisDistance * axisDistance;
    }
    return Math.sqrt(magnitude);
  }

  /**
   * 
   * @return cosine_similarity
   */
  private double computeCosineSimilarity(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {
    double cosine_similarity = 0.0;

    for (String word : queryVector.keySet()) {
      if (docVector.containsKey(word)) {
        cosine_similarity += queryVector.get(word) * docVector.get(word);
      }
    }
    cosine_similarity /= computeVectorMagnitude(queryVector) * computeVectorMagnitude(docVector);

    return cosine_similarity;
  }

  /**
   * 
   * @return mrr
   */
  private double compute_mrr(List<Integer> correctRankings) {
    double metric_mrr = 0.0;

    for (Integer ranking : correctRankings) {
      metric_mrr += 1.0 / ranking;
    }
    metric_mrr /= correctRankings.size();

    return metric_mrr;
  }

}
