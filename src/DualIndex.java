package ir;

import pagerank.PageRank;
import java.util.*;

public class DualIndex implements Index
{
    private BiwordIndex bi_index = new BiwordIndex(1);
    private MegaIndex mega_index = new MegaIndex(new LinkedList<String>(), 1);

    private static final double MEGA_WEIGHT = 1;
    private static final double K_LIMIT = 10;
    private static final String pathToLinks = "svwiki_links/links10000.txt";
    public int numberOfDocs = -2;
    private HashMap<String, Double> pageranks = new HashMap<String, Double>();

    public DualIndex()
    {
        System.err.println("Creating PageRank!");
        PageRank pr = new PageRank(pathToLinks);
        pageranks = pr.getPagerank();
        bi_index.setPagerank(pageranks);
        mega_index.setPagerank(pageranks);
        System.err.println("Done creating PageRank!");
    }

    public void insert(String token, int docID, int offset)
    {
        bi_index.insert(token, docID, offset);
        mega_index.insert(token, docID, offset);
    }

    public PostingsList getPostings(String token)
    {
        return mega_index.getPostings(token);
    }

    public PostingsList search_wo_sort(Query query, int queryType, int rankingType)
    {
        return null;
    }
    public PostingsList search(Query query, int queryType, int rankingType)
    {
        PostingsList bi_list = bi_index.search_wo_sort(query, queryType, rankingType);
        if(bi_list != null && bi_list.list.size() > K_LIMIT)
        {
            Collections.sort(bi_list.list);
            return bi_list;
        }
        PostingsList mega_list = mega_index.search_wo_sort(query, queryType, rankingType);
        if(bi_list == null && mega_list == null)
            return null;
        else if(bi_list == null)
        {
            Collections.sort(mega_list.list);
            return mega_list;
        }
        else if(mega_list == null)
        {
            Collections.sort(bi_list.list);
            return bi_list;
        }
        else
        {
            mega_list.merge_pl(bi_list, MEGA_WEIGHT);
            Collections.sort(mega_list.list);
            return mega_list;
        }
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

    public void setPagerank(HashMap<String, Double> new_pr)
    {
    }
}
