package ir;

import pagerank.PageRank;
import java.util.*;

public class BiwordIndex implements Index
{
    private HashMap<String, PostingsList> index = new HashMap<String, PostingsList>();
    private int current_docID = Integer.MIN_VALUE;
    private String last_term = null;

    private static final double PAGERANK_WEIGHT = 7;
    private static final String pathToLinks = "svwiki_links/links10000.txt";
    public int numberOfDocs = -2;
    private HashMap<String, Double> pageranks = new HashMap<String, Double>();

    public BiwordIndex()
    {
        System.err.println("Creating PageRank!");
        PageRank pr = new PageRank(pathToLinks);
        pageranks = pr.getPagerank();
        System.err.println("Done creating PageRank!");
    }
    
    public BiwordIndex(int input)
    {
    }

    public void insert(String token, int docID, int offset)
    {
        if(docID != current_docID)
        {
            last_term = token;
            current_docID = docID;
        }
        else
        {
            String term = create_biword(last_term, token);
            last_term = token;
            PostingsList current_list = index.get(term);
            if(current_list == null)
            {
                current_list = new PostingsList();
                index.put(term, current_list);
            }
            current_list.add(docID, offset);
        }
    }

    public PostingsList getPostings(String token)
    {
        return index.get(token);
    }

    public PostingsList search(Query query, int queryType, int rankingType)
    {
        PostingsList ret = search_wo_sort(query, queryType, rankingType);
        if(ret != null)
            Collections.sort(ret.list);
        return ret;
    }
    public PostingsList search_wo_sort(Query query, int queryType, int rankingType)
    {
        if(numberOfDocs < 0)
        {
            numberOfDocs = docLengths.keySet().size();
        }
        if(index == null)
            return null;
        if(query.terms.size() < 2)
        {
            return null;
        }
        if(queryType == Index.INTERSECTION_QUERY)
        {
            // If we have less than 2 words there are 
            // no biword to be constructed.
            String old_term = null;
            PostingsList result = null;;
            for(String term : query.terms)
            {
                if(old_term == null)
                {
                    old_term = term;
                }
                else
                {
                    String tmp = create_biword(old_term, term);
                    old_term = term;
                    PostingsList pl = getPostings(tmp);
                    if(pl == null)
                    {
                        System.out.println("Could not find biword.");
                        return null;
                    }
                    if(result == null)
                        result = pl;
                    else
                        result = PostingsList.intersect_query(result, getPostings(tmp));
                }
            }
            return result;
        }
        else if(queryType == Index.RANKED_QUERY)
        {    
            long startTime = System.nanoTime();

            ArrayList<String> terms = new ArrayList<String>();
            PostingsList result = null;
            String old_term = null;
            for(String term : query.terms)
            {
                if(old_term == null)
                    old_term = term;
                else
                {
                    String biword = create_biword(old_term, term);
                    PostingsList pl = getPostings(biword);
                    old_term = term;

                    if(pl == null)
                    {
                        continue;
                    }
                    terms.add(biword);

                    if(result == null)
                        result = pl;
                    else
                        result = PostingsList.union(result, pl);
                }
            }
            if(result == null) return null;
            if(rankingType == Index.TF_IDF || rankingType == Index.COMBINATION)
            {
                for(String term : terms)
                {
                    PostingsList tmp = (PostingsList) getPostings(term);
                    if(tmp == null) continue;

                    double idf = Math.log10( numberOfDocs / tmp.size() );

                    double wtq = (1.0 / (double) terms.size()) * idf;
                    for ( PostingsEntry pe : tmp.list )
                    {
                        if(pe.offsets.size() != 0)
                        {
                            //result.addScore(pe.docID, (1 + Math.log10(pe.offsets.size()))
                            //* idf * wtq);
                            result.addScore(pe.docID, pe.offsets.size() * idf * wtq);
                        }
                    }
                }
                for ( PostingsEntry pe : result.list )
                {
                    int length = docLengths.get(""+ pe.docID);
                    pe.score /= length;
                }
            }
            if(rankingType == Index.PAGERANK || rankingType == Index.COMBINATION)
            {
                if(pageranks == null)
                    return result;
                for(PostingsEntry pe : result.list)
                {
                    String tmpStr = docIDs.get("" + pe.docID);
                    tmpStr = tmpStr.substring(tmpStr.lastIndexOf('/') + 1,
                            tmpStr.lastIndexOf('.'));
                    result.addScore(pe.docID, ((Double) pageranks.get(tmpStr))
                            * PAGERANK_WEIGHT);
                }
            }
            //Collections.sort(result.list);
            System.out.println("This query took " + (System.nanoTime() - startTime));
            return result;
        }
        return null;
    }

    public void cleanup()
    {
    }

    public int getNumberOfDocs()
    {
        if(numberOfDocs < 0)
        {
            numberOfDocs = docLengths.keySet().size(); 
        }
        return numberOfDocs;
    }

    public void addTerm(int docID, String token)
    {
        HashSet<String> tmp = terms.get(docID);
        if(tmp == null)
        {
            tmp = new HashSet<String>();
            terms.put(docID, tmp);
        }
        tmp.add(token);
    }

    public PostingsList getPostings()
    {
        return null;
    }

    private String create_biword(String a, String b)
    {
        if(a == null || b == null)
            return null;
        else
            return "{" + a + "+" + b + "}";
    }

    public void setPagerank(HashMap<String, Double> new_pr)
    {
        pageranks = new_pr;
    }
}
