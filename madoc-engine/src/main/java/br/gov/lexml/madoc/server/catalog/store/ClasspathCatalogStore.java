package br.gov.lexml.madoc.server.catalog.store;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import br.gov.lexml.madoc.server.schema.Constants;

public class ClasspathCatalogStore implements DocumentStore {

	private static final Logger log = LoggerFactory.getLogger(ClasspathCatalogStore.class);

	private ClasspathCatalogStoreMetadataProcessor metadataProcessor = new ClasspathCatalogStoreMetadataProcessor();

	private final String catalogUri;
	private final String packageName;

	public ClasspathCatalogStore(String catalogUri, String packageName) {
		super();
		this.catalogUri = catalogUri;
		this.packageName = packageName;
	}

	@Override
	public InputStream getDocument(String docUri) throws IOException {

		log.debug("ClasspathCatalogStore: " + docUri);

		if (!docUri.equals(catalogUri)) {
			if(docUri.startsWith(packageName)) {
				return getResourceAsStream(docUri);
			}
			return null;
		}
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();

			// creating Catalog
			Document resDoc = db.newDocument();
			Element catalogElement = resDoc.createElementNS(Constants.DEFAULT_URI,
					Constants.SHORT_NAMESPACE + "Catalog");
			resDoc.appendChild(catalogElement);

			// processing catalog content
			processCatalogElements(db, resDoc, catalogElement);

			// finalization
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer t = tf.newTransformer();
			DOMSource ds = new DOMSource(resDoc);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			StreamResult res = new StreamResult(bos);
			t.transform(ds, res);
			IOUtils.closeQuietly(bos);
			return new ByteArrayInputStream(bos.toByteArray());
		} catch (XPathExpressionException e) {
			throw new RuntimeException("Error compiling xpath expression: " + e.getMessage(), e);
		} catch (SAXException e) {
			throw new RuntimeException("Error processing file. " + e.getMessage(), e);
		} catch (TransformerConfigurationException e) {
			throw new RuntimeException("Error configuring rendering: " + e.getMessage(), e);
		} catch (TransformerException e) {
			throw new RuntimeException("Error rendering document: " + e.getMessage(), e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Error configuring parser: " + e.getMessage(), e);
		}
	}

	private void processCatalogElements(DocumentBuilder db, Document resDoc, Element catalogElement)
			throws SAXException, IOException, XPathExpressionException {

		// parsing files
		Items itens = processItems(db);

		// creating MadocDocuments tag
		Element madocDocuments = getMadocDocumentBaseCatalogItemType(Constants.MADOC_DOCUMENTS_CATALOG_ELEMENT,
				Constants.MADOC_DOCUMENT_ROOT_ELEMENT, resDoc, itens);
		if (madocDocuments != null) {
			catalogElement.appendChild(madocDocuments);
		}

		// creating MadocSkeleton tag
		Element skeletonDocuments = getMadocDocumentBaseCatalogItemType(Constants.MADOC_SKELETONS_CATALOG_ELEMENT,
				Constants.MADOC_SKELETON_ROOT_ELEMENT, resDoc, itens);
		if (skeletonDocuments != null) {
			catalogElement.appendChild(skeletonDocuments);
		}

		// creating MadocLibrary tag
		Element libraryDocuments = getMadocDocumentBaseCatalogItemType(Constants.MADOC_LIBRARIES_CATALOG_ELEMENT,
				Constants.MADOC_LIBRARY_ROOT_ELEMENT, resDoc, itens);
		if (skeletonDocuments != null) {
			catalogElement.appendChild(libraryDocuments);
		}

		// creating Resources tag
		Element resources = getMadocDocumentBaseCatalogItemType(Constants.RESOURCES_CATALOG_ELEMENT,
				Constants.RESOURCE_ELEMENT, resDoc, itens);
		if (resources != null) {
			catalogElement.appendChild(resources);
		}

	}

	/**
	 * Process modelDirectory and build an Items object with lists of
	 * MadocDocument, SkeletonDocument, LibraryDocument and Resources.
	 * 
	 * @param db
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 */
	private Items processItems(DocumentBuilder db) throws SAXException, IOException {

		Items itens = new Items();

		List<String> resNames = getResourceFiles(packageName);
		
		// processing XML items
		
		for(String resName: resNames) {
			if(resName.toLowerCase().endsWith("xml")) {
//				String str = IOUtils.toString(getResourceAsStream(packageName + resName));
//				Document doc = db.parse(new StringInputStream(str));
				Document doc = db.parse(getResourceAsStream(packageName + resName));

				String rootElementName = doc.getDocumentElement().getNodeName();
				if (rootElementName != null) {
					rootElementName = rootElementName.replaceFirst("^.*:", "");
				}
				if (rootElementName.equals(Constants.MADOC_DOCUMENT_ROOT_ELEMENT)
						|| rootElementName.equals(Constants.MADOC_SKELETON_ROOT_ELEMENT)
						|| rootElementName.equals(Constants.MADOC_LIBRARY_ROOT_ELEMENT)) {
					itens.add(rootElementName, resName, doc);
				}
			}
		}

		// processing resources documents

		for(String resName: resNames) {
			if(!resName.toLowerCase().endsWith("xml")) {
				itens.add(resName);
			}
		}

		return itens;
	}

	private Element getMadocDocumentBaseCatalogItemType(String tagName, String type, Document resDoc, Items itens)
			throws XPathExpressionException {
		Element skeletonDocuments = resDoc.createElementNS(Constants.DEFAULT_URI, Constants.SHORT_NAMESPACE + tagName);
		// catalogElement.appendChild(skeletonDocuments);

		for (Item item : itens.getItens(type)) {
			Element e = metadataProcessor.getMetadataElement(item.doc);
			if (e != null) {
				Element itemElement = resDoc.createElementNS(Constants.DEFAULT_URI, Constants.SHORT_NAMESPACE + type);

				if (!StringUtils.isEmpty(item.version)) {
					itemElement.setAttribute("version", item.version);
				}
				itemElement.setAttribute("resourceName", item.resourceName);
				itemElement.setAttribute("fileSuffix", item.fileSuffix);
//				itemElement.setAttribute("mimetype", item.mimetype);
//				itemElement.setAttribute("size", Long.toString(item.size));
				itemElement.setAttribute("obsolete", item.obsolete ? "true" : "false");
				itemElement.appendChild(resDoc.importNode(e, true));

				skeletonDocuments.appendChild(itemElement);
			}
		}

		return skeletonDocuments;
	}

	private List<String> getResourceFiles(String path) throws IOException {
		List<String> filenames = new ArrayList<>();

		try (InputStream in = getResourceAsStream(path);
				BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
			String resource;
			while ((resource = br.readLine()) != null) {
				filenames.add(resource);
			}
		}

		return filenames;
	}

	private InputStream getResourceAsStream(String resource) {
		final InputStream in = getContextClassLoader().getResourceAsStream(resource);

		return in == null ? getClass().getResourceAsStream(resource) : in;
	}

	private ClassLoader getContextClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}

	static class Items implements Serializable {

		private static final Pattern classpathResourcePattern = Pattern.compile("^(.*)(?:_([\\d\\.]+))?.(?:xml|XML)$");

		Map<String, List<Item>> docsList = new HashMap<String, List<Item>>();

		/**
		 * Add a XML document
		 * 
		 * @param type
		 * @param file
		 * @param doc
		 */
		public void add(String type, String classpathResource, Document doc) {

			if (!docsList.containsKey(type)) {
				docsList.put(type, new ArrayList<Item>());
			}

			Item item = createItem(classpathResource, doc);
			if (item != null) {
				docsList.get(type).add(item);
			}
		}

		/**
		 * Add a resource
		 * 
		 * @param type
		 * @return
		 */
		public void add(String classpathResource) {
			add(Constants.RESOURCES_CATALOG_ELEMENT, classpathResource, null);
		}

		public List<Item> getItens(String type) {
			List<Item> itens = docsList.get(type);

			if (itens == null) {
				return new ArrayList<Item>();
			}

			return itens;
		}

		private Item createItem(String classpathResource, Document doc) {

			Matcher matcher = classpathResourcePattern.matcher(classpathResource);
			if (matcher.matches()) {
				String resourceName = matcher.group(1);
				String version = matcher.group(2);

				String suffix = FilenameUtils.getExtension(classpathResource);

				Item item = new Item();

				item.classpathResource = classpathResource;
				item.doc = doc;
				item.resourceName = resourceName;
				item.version = version;
				item.fileSuffix = suffix;
				item.obsolete = false;

				return item;
			} else {
				return null;
			}
		}

	}

	static class Item implements Serializable {
		String resourceName;
		String version;
		String fileSuffix;
		String classpathResource;
		Document doc;
		boolean obsolete;
	}
}
