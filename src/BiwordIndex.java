package ir;

import pagerank.PageRank;
import java.util.*;

public class BiwordIndex implements Index
{
    private HashMap<String, PostingsList> index = new HashMap<String, PostingsList>();
    private int current_docID = Integer.MIN_VALUE;
    private String last_term = null;

    private static final String pathToLinks = "svwiki_links/links10000.txt";
    private HashMap<String, Double> pageranks = new HashMap<String, Double>();
    public int numberOfDocs = -2;

    public BiwordIndex()
    {
        System.err.println("Creating PageRank!");
        PageRank pr = new PageRank(pathToLinks);
        pageranks = pr.getPagerank();
        System.err.println("Done creating PageRank!");
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
        if(index == null)
            return null;
        if(queryType == Index.INTERSECTION_QUERY)
        {
            // If we have less than 2 words there are 
            // no biword to be constructed.
            if(query.terms.size() < 2)
            {
                return null;
            }
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
                    PostingsList pl = getPostings(tmp);
                    if(tmp == null)
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
            return a + "+" + b;
    }

    private String get_first(String term)
    {
        return term.substring(0, term.indexOf('+'));
    }

    private String get_second(String term)
    {
        return term.substring(term.indexOf('+') + 1);
    }
}
