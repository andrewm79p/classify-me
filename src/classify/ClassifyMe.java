package classify;

import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Scanner;
import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.*;

public class ClassifyMe {

	private static String appId = "Iu5udbvV34Fcg3uDwfJMTEY8Lb09.yMmFTaf7axWid3g4LmEN3G3iBUs6pa6jrRE";
	private String databaseURL;
	private double specificity;
	private int coverage;
	private Vector<Category> classification;
	private Vector<String> categoryPaths;
	private Category root;
	private int count;
	private int sampleSize = 4;

	/**
	 * Initialize the category tree
	 */
	public ClassifyMe() {

		root = new Category();
		root.name = "Root";
		root.specificity = 1;
		root.subcategories = new Vector<Category>();
		root.parent = null;

		Category hardware = new Category();
		hardware.name = "Hardware";

		Category programming = new Category();
		programming.name = "Programming";

		Category computers = new Category();
		computers.name = "Computers";
		computers.subcategories = new Vector<Category>();
		computers.subcategories.add(hardware);
		computers.subcategories.add(programming);
		hardware.parent = computers;
		programming.parent = computers;
		computers.parent = root;
		root.subcategories.add(computers);

		Category fitness = new Category();
		fitness.name = "Fitness";

		Category diseases = new Category();
		diseases.name = "Diseases";

		Category health = new Category();
		health.name = "Health";
		health.subcategories = new Vector<Category>();
		health.subcategories.add(fitness);
		health.subcategories.add(diseases);
		fitness.parent = health;
		diseases.parent = health;
		health.parent = root;
		root.subcategories.add(health);

		Category basketball = new Category();
		basketball.name = "Basketball";

		Category soccer = new Category();
		soccer.name = "Soccer";

		Category sports = new Category();
		sports.name = "Sports";
		sports.subcategories = new Vector<Category>();
		sports.subcategories.add(basketball);
		sports.subcategories.add(soccer);
		basketball.parent = sports;
		soccer.parent = sports;
		sports.parent = root;
		root.subcategories.add(sports);

		classification = new Vector<Category>();
		categoryPaths = new Vector<String>();

		root.printTree();
	}

	/**
	 * return current classification path variable value
	 * 
	 * @return current classification path
	 */
	protected String getClassificationPaths() {
		String res = "";
		for (int i = 0; i < categoryPaths.size(); i++) {
			res = res + categoryPaths.get(i) + ", ";
		}
		if (res.length() >= 2)
			res = res.substring(0, res.length() - 2);
		return res;
	}

	/**
	 * Get the i^th Category path the database belongs to and make summary based
	 * on its classification
	 * 
	 * @param c
	 *            the Child class node that database belongs to
	 */
	protected void getCategoryPath(Category c, int i) {
		// if current node has parents, we will recursively call this function
		// to get the full path
		if (c.parent != null) {
			getCategoryPath(c.parent, i);
//			if (c.subcategories != null) { // delete this line if it works without it
				if (c.parent.subclassCategories == null) 
					c.parent.subclassCategories = new HashSet<Category>();
				c.parent.subclassCategories.add(c);
				c.parent.needToExtractSummary = true;
				System.out.println("Adding " + c.name + " to " + c.parent.name);
//			}
			categoryPaths.set(i, categoryPaths.get(i) + '/');
		}
		categoryPaths.set(i, categoryPaths.get(i) + c.name);
	}
	
/*	
	// this method populates the subclassCategories field in Categories along the classification paths
	private void prepareBeforeExtraction() {
		for (int k = 0; k < classification.size(); k++) {
			populateSubclassCategories(classification.elementAt(k));
//			System.out.println("Populating from Category: " + classification.elementAt(k).name);
		}		
	}
	
	private void populateSubclassCategories(Category c) {
		// if this is not the root we can populate up the classification tree
		if (c.parent != null ) {
			if (c.parent.subclassCategories != null) {
//				if (!c.parent.subclassCategories.contains(c)) {
				    // if an object is in the set 'adding' it does not change anything
					c.parent.subclassCategories.add(c);
					System.out.println("Adding " + c.name + " to " + c.parent.name);
//				}				
			} else {
				c.parent.subclassCategories = new HashSet<Category>();
				c.parent.subclassCategories.add(c);
				System.out.println("Adding " + c.name + " to " + c.parent.name);
			}
			c.parent.needToExtractSummary = true;
		}
	}
*/
	private void extractAllSummaries(Category c) {
		compoundExtractSummary(c);
		if (c.subcategories != null) {
			for(int k = 0; k < c.subcategories.size(); k++) {
				extractAllSummaries(c.subcategories.elementAt(k));
			}
		}
	}

	/**
	 * This function will extract summary for given database for category if we need to extract summary
	 * (the samples will be combined from the populated subclassCategories)
	 * @param c
	 *            current category node
	 */
	private void compoundExtractSummary(Category cat) {
		if (cat.needToExtractSummary) {
			System.out.println("Building summary for category:" + cat.name);
			Hashtable<String, Integer> catSamples = new Hashtable<String, Integer>();			
			Hashtable<String, Integer> sum = new Hashtable<String, Integer>();
			// temporary set to store the string set returned from the lynx
			// function
			Set<String> tempWords;
			String tempWord;
			Integer innerTempValue;
			String tempUrl;
			Iterator<String> innerIterator;
			// use tree set to sort words in alphabetical order
			Set<String> keys = new TreeSet<String>();
			Category c;
			
			for (Iterator<Category> iter = cat.subclassCategories.iterator(); iter.hasNext(); ) {
				c = iter.next();
				// get sample urls of current category node
				Iterator<String> iterator = c.samples.keySet().iterator();
				while (iterator.hasNext()) {
					tempUrl = (String) (iterator.next());
					// make sure no duplicated url from different queries
					if (!catSamples.containsKey(tempUrl)) {
						catSamples.put(tempUrl, 1);
						System.out.println("Getting Page : " + tempUrl + "\n\n");
						tempWords = GetWordsLynx.runLynx(tempUrl);
						innerIterator = tempWords.iterator();
						// Calculate document frequency for each word
						while (innerIterator.hasNext()) {
							tempWord = innerIterator.next();
							if (sum.containsKey(tempWord)) {
								innerTempValue = (Integer) sum.get(tempWord);
								innerTempValue = innerTempValue + 1;
								sum.put(tempWord, innerTempValue);
							} else {
								sum.put(tempWord, 1);
								keys.add(tempWord);
							}
						}
					}
				}				
			}
			// print the words statistic data to file
			FileOutputStream output;
			try {
				File f = new File(cat.name + "-" + databaseURL + ".txt");
				if (f.exists())
					f.delete();
				output = new FileOutputStream(cat.name + "-" + databaseURL
						+ ".txt");
				PrintStream file = new PrintStream(output);
				System.out.println("Category: " + cat.name + ", Keys size : "
						+ keys.size());
				Iterator<String> iterator = keys.iterator();
				while (iterator.hasNext()) {
					tempWord = iterator.next();
					file.println(tempWord + "#" + sum.get(tempWord));
				}
				output.close();
				System.out.println("Writing summary to file: " + cat.name + "-"
						+ databaseURL + ".txt");
				cat.extractedSummary = true;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * This function will extract summary for given database for each category
	 * 
	 * @param c
	 *            current category node
	 */
	private void extractSummary(Category c) {
		if (c.parent != null) {
			extractSummary(c.parent);
		}
		// if this category is leaf so don't build content summary
		// so if it is not a leaf and summary has not been built
		if (!c.extractedSummary && c.subcategories != null) {
			// make summary on current category node
			// hashtable sum is used for counting words
			System.out.println("Building summary for category:" + c.name);

			Hashtable<String, Integer> catSamples = new Hashtable<String, Integer>();

			Hashtable<String, Integer> sum = new Hashtable<String, Integer>();
			// temporary set to store the string set returned from the lynx
			// function
			Set<String> tempWords;
			String tempWord;
			Integer innerTempValue;
			String tempUrl;
			Iterator<String> innerIterator;

			// use tree set to sort words in alphabetical order
			Set<String> keys = new TreeSet<String>();
			// get sample urls of current category node
			Iterator<String> iterator = c.samples.keySet().iterator();
			while (iterator.hasNext()) {
				tempUrl = (String) (iterator.next());
				// make sure no duplicated url from different queries
				if (!catSamples.containsKey(tempUrl)) {
					catSamples.put(tempUrl, 1);
					System.out.println("Getting Page : " + tempUrl + "\n\n");
					tempWords = GetWordsLynx.runLynx(tempUrl);
					innerIterator = tempWords.iterator();
					// Calculate document frequency for each word
					while (innerIterator.hasNext()) {
						tempWord = innerIterator.next();
						if (sum.containsKey(tempWord)) {
							innerTempValue = (Integer) sum.get(tempWord);
							innerTempValue = innerTempValue + 1;
							sum.put(tempWord, innerTempValue);
						} else {
							sum.put(tempWord, 1);
							keys.add(tempWord);
						}
					}
				}
			}
			// print the words statistic data to file
			FileOutputStream output;
			try {
				File f = new File(c.name + "-" + databaseURL + ".txt");
				if (f.exists())
					f.delete();
				output = new FileOutputStream(c.name + "-" + databaseURL
						+ ".txt");
				PrintStream file = new PrintStream(output);
				System.out.println("Category: " + c.name + ", Keys size : "
						+ keys.size());
				iterator = keys.iterator();
				while (iterator.hasNext()) {
					tempWord = iterator.next();
					file.println(tempWord + "#" + sum.get(tempWord));
				}
				output.close();
				System.out.println("Writing summary to file: " + c.name + "-"
						+ databaseURL + ".txt");
				c.extractedSummary = true;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Classify database from category node c
	 * 
	 * @param c
	 *            Category to start with for classification
	 * @return Return the child node category that database belong to
	 */
	private Vector<Category> classify(Category c) {
		Vector<Category> catList = new Vector<Category>();
		try {
			String categoryName = c.name;
			if (c.subcategories == null) {
				// System.out.println("This category is a leaf");
				catList.add(c);
				return catList;
			}

			c.coverage = 0;
			for (int j = 0; j < c.subcategories.size(); j++) {
				c.subcategories.elementAt(j).coverage = 0;
			}

			File f = new File(categoryName.toLowerCase() + ".txt");
			if (!f.exists()) {
				System.err
						.println("File "
								+ categoryName.toLowerCase()
								+ ".txt"
								+ " is not found. It should provide the queries for category."
								+ categoryName.toLowerCase());
			}
			BufferedReader input = new BufferedReader(new FileReader(
					categoryName.toLowerCase() + ".txt"));
			String line = null;
			try {
				int index;
				String subcatName;
				String probingQuery;
				File dir = new File("cache/sample-"+c.name + "-" +databaseURL +"/");
				if (dir.exists())
					dir.delete();
				new File("cache/sample-"+c.name + "-" +databaseURL).mkdir();
				while ((line = input.readLine()) != null) {
					index = line.indexOf((int) ' ');
					if (index < 0) {
						System.err
								.println("No space found. Error parsing a line containing category and probing query");
						index = 0;
					}
					subcatName = line.substring(0, index);
					probingQuery = line.substring(index + 1);
					probingQuery = probingQuery.trim();
					// we want to get rid of white spaces at the end or
					// beginning of the string
					c.queries.addElement(probingQuery);
					c = probe(probingQuery, c);
					c.coverage += count;
					for (int j = 0; j < c.subcategories.size(); j++) {
						if (c.subcategories.elementAt(j).name
								.equals(subcatName)) {
							c.subcategories.elementAt(j).coverage += count;
						}
					}

				}
				input.close();

				// iterate category tree to find class for database by recursive
				// calling classify()
				for (int j = 0; j < c.subcategories.size(); j++) {
					c.subcategories.elementAt(j).specificity = (c.specificity)
							* (c.subcategories.elementAt(j).coverage)
							/ c.coverage;
					System.out.println("Specificity for category:"
							+ c.subcategories.elementAt(j).name + " is "
							+ c.subcategories.elementAt(j).specificity);
					System.out.println("Coverage for category:"
							+ c.subcategories.elementAt(j).name + " is "
							+ c.subcategories.elementAt(j).coverage);
					if (c.subcategories.elementAt(j).specificity > specificity
							&& c.subcategories.elementAt(j).coverage > coverage) {
						catList.addAll(classify(c.subcategories.elementAt(j)));
					}

				}
				if (catList.size() == 0) {
					catList.add(c);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return catList;
	}

	/**
	 * Implement QProbe algorithm
	 * 
	 * @param query
	 *            The query for Prober
	 * @param c
	 *            The current node for Prober query
	 * @return Return updated category node (Update samples)
	 */
	private Category probe(String query, Category c) {
		// replace all spaces with "%20" to get url friendly query
		String urlQuery = query.replaceAll(" ", "%20");
		URL url;
		try {
			url = new URL("http://boss.yahooapis.com/ysearch/web/v1/"
					+ urlQuery + "?appid=" + appId + "&format=xml&sites="
					+ databaseURL);

			URLConnection con = url.openConnection();
			InputStream inStream = con.getInputStream();
			Scanner in = new Scanner(inStream);
			StringBuffer temp = new StringBuffer();
			while (in.hasNextLine()) {
				temp.append(in.nextLine());
			}
			String res = temp.toString();
			return parseSearchResult(res, c, query);

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return c;
	}

	/**
	 * Anaylze the XML for search result returned from Yahoo! to find out result
	 * items and total hits
	 * 
	 * @param response
	 *            Response string (XML)
	 * @param c
	 *            Category node for such search
	 * @return Return updated category (update samples and global varible count)
	 */
	private Category parseSearchResult(String response, Category c, String query) {
		SearchResult sr;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(
					response)));
			NodeList nodeL = doc.getElementsByTagName("resultset_web");
			Node node = nodeL.item(0);
			NamedNodeMap nodeMap = node.getAttributes();
			node = nodeMap.getNamedItem("totalhits");
			count = Integer.parseInt(node.getTextContent());

			// write down the number of matches found for this query
			File f = new File("cache/" + databaseURL + '/' + query + ".txt");
			if (f.exists()) {
				BufferedReader input = new BufferedReader(new FileReader(
						"cache/" + databaseURL + '/' + query + ".txt"));
				String line;
				if ((line = input.readLine()) != null
						&& Integer.parseInt(line) == count) {

				} else {
					System.err.println("Problem verifing query hit count.");
					System.err.println("query: " + query);
					System.err.println("old count:" + line + " new count:"
							+ count);
				}
				input.close();
			} else {
				// if the cache does not exist we need to create the appropriate
				// folders first
				FileOutputStream output;
				f = new File("cache/");
				if (!f.exists()) {
					f.mkdir();
					f = new File("cache/" + databaseURL);
					f.mkdir();
				} else {
					f = new File("cache/" + databaseURL);
					if (!f.exists())
						f.mkdir();
				}
				output = new FileOutputStream("cache/" + databaseURL + '/'
						+ query + ".txt");
				PrintStream file = new PrintStream(output);
				file.println(count);
				output.close();
			}
			f = new File("sample-" + c.name + "-" + databaseURL);
			if (!f.exists())
				f.mkdir();
			FileOutputStream output = new FileOutputStream("sample-" + c.name + "-" + databaseURL +"/" + query
					+ "_urls.txt");
			nodeL = doc.getElementsByTagName("result");
			PrintStream file = new PrintStream(output);
			for (int i = 0; i < sampleSize; i++) {
				node = nodeL.item(i);
				if (node != null) {
					sr = new SearchResult(node);
					file.println(sr.url);
					if (!c.samples.containsKey(sr.url))
						c.samples.put(sr.url, 1);

				}
			}
			output.close();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return c;
	}
	
	private void showme(Category c) {
	    System.out.println(c.name + ", ");
	    if (c.subclassCategories != null) {
	    	for (Iterator<Category> iter = c.subclassCategories.iterator(); iter.hasNext(); ) {
			    String name = iter.next().name;
			    System.out.print(name + "#");
			}
	    }
	    if (c.subcategories == null) return;
		for (int k = 0; k < c.subcategories.size(); k++) {
			showme(c.subcategories.elementAt(k));
		}
	}


	public static void main(String args[]) {
	
		if (args.length != 3) {
			System.out
					.println("Usage: ClassifyMe <database-url> <specificity> <coverage> <yahoo appId>");
			System.exit(1);
		}
		ClassifyMe cm = new ClassifyMe();		
		cm.databaseURL = args[0];
		File f = new File("cache/" + cm.databaseURL);
		if (f.exists()) {
			File[] files = f.listFiles();
			for (int i = 0; i < files.length; i++) {
				files[i].delete();
			}
			System.out.println("The old cache directory has been removed.");
		}
		cm.specificity = Double.parseDouble(args[1]);
		cm.coverage = Integer.parseInt(args[2]);
		System.out.println("Classifying...");
		cm.classification = cm.classify(cm.root);
		for (int k = 0; k < cm.classification.size(); k++) {
			cm.categoryPaths.add("");
			cm.getCategoryPath(cm.classification.elementAt(k), k);
			// if there are children categories to the classification cat c include them in the subclassCat of this cat c
			// so we use their samples when extracting summaries
			if (cm.classification.elementAt(k).subcategories != null) {
				cm.classification.elementAt(k).needToExtractSummary = true;
				cm.classification.elementAt(k).subclassCategories.addAll(cm.classification.elementAt(k).subcategories);
			}
		}

		System.out.println("\n\nClassification: " + cm.getClassificationPaths()
				+ "\n\n");
		
//		cm.prepareBeforeExtraction();		
//		cm.showme(cm.root);
		cm.extractAllSummaries(cm.root);
/*
		for (int k = 0; k < cm.classification.size(); k++) {
			cm.compoundExtractSummary(cm.classification.elementAt(k));
		}
*/
	}
}
