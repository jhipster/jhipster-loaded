package io.github.jhipster.loaded.reloader.liquibase;

import liquibase.changelog.ChangeSet;
import liquibase.parser.core.xml.LiquibaseEntityResolver;
import liquibase.parser.core.xml.XMLChangeLogSAXParser;
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
import java.util.List;

/**
 * Override the serializer to add the logicalFilePath at the databaseChangeLog and the changeSet elements
 * This will fix the issues when the changeSet is running twice during the hotreload and when the application starts
 */
public class CustomXMLChangeLogSerializer extends XMLChangeLogSerializer {
    @Override
    public void write(List<ChangeSet> changeSets, OutputStream out) throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder;
        try {
            documentBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        documentBuilder.setEntityResolver(new LiquibaseEntityResolver(new CustomXMLChangeLogSerializer()));

        Document doc = documentBuilder.newDocument();
        Element changeLogElement = doc.createElementNS("http://www.liquibase.org/xml/ns/dbchangelog", "databaseChangeLog");

        changeLogElement.setAttribute("logicalFilePath", "none");
        changeLogElement.setAttribute("xmlns","http://www.liquibase.org/xml/ns/dbchangelog");
        changeLogElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        changeLogElement.setAttribute("xsi:schemaLocation", "http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-" + XMLChangeLogSAXParser.getSchemaVersion() + ".xsd");

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
