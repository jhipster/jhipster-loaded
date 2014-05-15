package io.github.jhipster.loaded.reloader.liquibase;

import liquibase.change.ChangeFactory;
import liquibase.changelog.ChangeSet;
import liquibase.parser.NamespaceDetails;
import liquibase.parser.NamespaceDetailsFactory;
import liquibase.parser.core.xml.LiquibaseEntityResolver;
import liquibase.serializer.LiquibaseSerializable;
import liquibase.serializer.core.xml.XMLChangeLogSerializer;
import liquibase.util.xml.DefaultXmlWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Override the serializer to add the logicalFilePath at the databaseChangeLog and the changeSet elements
 * This will fix the issues when the changeSet is running twice during the hotreload and when the application starts
 */
public class CustomXMLChangeLogSerializer extends XMLChangeLogSerializer {


    @Override
    public void write(List<ChangeSet> changeSets, OutputStream out) throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder documentBuilder;
        try {
            documentBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        documentBuilder.setEntityResolver(new LiquibaseEntityResolver(this));

        Document doc = documentBuilder.newDocument();
        Element changeLogElement = doc.createElementNS(LiquibaseSerializable.STANDARD_CHANGELOG_NAMESPACE, "databaseChangeLog");

        changeLogElement.setAttribute("logicalFilePath", "none");
        changeLogElement.setAttribute("xmlns", LiquibaseSerializable.STANDARD_CHANGELOG_NAMESPACE);
        changeLogElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");

        Map<String, String> shortNameByNamespace = new HashMap<String, String>();
        Map<String, String> urlByNamespace = new HashMap<String, String>();
        for (String namespace : ChangeFactory.getInstance().getAllChangeNamespaces()) {
            NamespaceDetails details = NamespaceDetailsFactory.getInstance().getNamespaceDetails(this, namespace);
            if (details != null) {
                shortNameByNamespace.put(namespace, details.getShortName(namespace));
                urlByNamespace.put(namespace, details.getSchemaUrl(namespace));
            }
        }

        for (Map.Entry<String, String> entry : shortNameByNamespace.entrySet()) {
            if (!entry.getValue().equals("")) {
                changeLogElement.setAttribute("xmlns:"+entry.getValue(), entry.getKey());
            }
        }


        String schemaLocationAttribute = "";
        for (Map.Entry<String, String> entry : urlByNamespace.entrySet()) {
            if (!entry.getValue().equals("")) {
                schemaLocationAttribute += entry.getKey()+" "+entry.getValue()+" ";
            }
        }

        changeLogElement.setAttribute("xsi:schemaLocation", schemaLocationAttribute.trim());

        doc.appendChild(changeLogElement);
        setCurrentChangeLogFileDOM(doc);

        for (ChangeSet changeSet : changeSets) {
            doc.getDocumentElement().appendChild(createNode(changeSet));
        }

        new DefaultXmlWriter().write(doc, out);
    }

    @Override
    public Element createNode(LiquibaseSerializable object) {
        Element node = super.createNode(object);

        if (object instanceof ChangeSet) {
            node.setAttribute("logicalFilePath", "none");
        }

        return node;
    }
}
