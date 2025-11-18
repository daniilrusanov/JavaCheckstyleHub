package com.checkstylehub.analyzer.service;

import com.checkstylehub.analyzer.dto.CheckstyleRulesDto;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * Service for converting between Checkstyle XML configuration and structured CheckstyleRulesDto.
 * Handles parsing XML to DTO and generating XML from DTO.
 */
@Service
public class CheckstyleXmlConverter {

    /**
     * Parses Checkstyle XML configuration into a structured DTO.
     *
     * @param xmlContent the XML configuration content
     * @return CheckstyleRulesDto containing parsed rules
     */
    public CheckstyleRulesDto parseXmlToDto(String xmlContent) {
        CheckstyleRulesDto dto = new CheckstyleRulesDto();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();

            dto.setCharset(getPropertyValue(root, "charset", "UTF-8"));
            dto.setSeverity(getPropertyValue(root, "severity", "warning"));
            dto.setFileExtensions(getPropertyValue(root, "fileExtensions", "java, properties, xml"));

            Element lineLength = findModuleByName(root, "LineLength");
            if (lineLength != null) {
                String maxValue = getPropertyValue(lineLength, "max", "120");
                dto.setLineLength(Integer.parseInt(maxValue));
                dto.setLineLengthIgnorePattern(getPropertyValue(lineLength, "ignorePattern", ""));
            }

            Element treeWalker = findModuleByName(root, "TreeWalker");
            if (treeWalker != null) {
                dto.setOuterTypeFilename(hasModule(treeWalker, "OuterTypeFilename"));
                dto.setIllegalTokenText(hasModule(treeWalker, "IllegalTokenText"));
                dto.setAvoidEscapedUnicodeCharacters(hasModule(treeWalker, "AvoidEscapedUnicodeCharacters"));
                dto.setAvoidStarImport(hasModule(treeWalker, "AvoidStarImport"));
                dto.setOneTopLevelClass(hasModule(treeWalker, "OneTopLevelClass"));
                dto.setNoLineWrap(hasModule(treeWalker, "NoLineWrap"));
                dto.setEmptyBlock(hasModule(treeWalker, "EmptyBlock"));
                dto.setNeedBraces(hasModule(treeWalker, "NeedBraces"));
                dto.setLeftCurly(hasModule(treeWalker, "LeftCurly"));
                dto.setRightCurly(hasModule(treeWalker, "RightCurly"));
                dto.setEmptyStatement(hasModule(treeWalker, "EmptyStatement"));
                dto.setEqualsHashCode(hasModule(treeWalker, "EqualsHashCode"));
                dto.setIllegalInstantiation(hasModule(treeWalker, "IllegalInstantiation"));
                dto.setMissingSwitchDefault(hasModule(treeWalker, "MissingSwitchDefault"));
                dto.setSimplifyBooleanExpression(hasModule(treeWalker, "SimplifyBooleanExpression"));
                dto.setSimplifyBooleanReturn(hasModule(treeWalker, "SimplifyBooleanReturn"));
                dto.setFinalClass(hasModule(treeWalker, "FinalClass"));
                dto.setHideUtilityClassConstructor(hasModule(treeWalker, "HideUtilityClassConstructor"));
                dto.setInterfaceIsType(hasModule(treeWalker, "InterfaceIsType"));
                dto.setVisibilityModifier(hasModule(treeWalker, "VisibilityModifier"));
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse XML configuration: " + e.getMessage(), e);
        }

        return dto;
    }

    /**
     * Generates Checkstyle XML configuration from a structured DTO.
     *
     * @param dto the rules DTO
     * @return XML configuration as string
     */
    public String generateXmlFromDto(CheckstyleRulesDto dto) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element checker = doc.createElement("module");
            checker.setAttribute("name", "Checker");
            doc.appendChild(checker);

            addProperty(doc, checker, "charset", dto.getCharset());
            addProperty(doc, checker, "severity", dto.getSeverity());
            addProperty(doc, checker, "fileExtensions", dto.getFileExtensions());

            if (dto.getLineLength() != null) {
                Element lineLength = doc.createElement("module");
                lineLength.setAttribute("name", "LineLength");
                addProperty(doc, lineLength, "max", String.valueOf(dto.getLineLength()));
                if (dto.getLineLengthIgnorePattern() != null && !dto.getLineLengthIgnorePattern().isEmpty()) {
                    addProperty(doc, lineLength, "ignorePattern", dto.getLineLengthIgnorePattern());
                }
                checker.appendChild(lineLength);
            }

            Element treeWalker = doc.createElement("module");
            treeWalker.setAttribute("name", "TreeWalker");
            checker.appendChild(treeWalker);

            if (Boolean.TRUE.equals(dto.getOuterTypeFilename())) {
                addModule(doc, treeWalker, "OuterTypeFilename");
            }

            if (Boolean.TRUE.equals(dto.getIllegalTokenText())) {
                Element illegalTokenText = doc.createElement("module");
                illegalTokenText.setAttribute("name", "IllegalTokenText");
                addProperty(doc, illegalTokenText, "tokens", "STRING_LITERAL, CHAR_LITERAL");
                addProperty(doc, illegalTokenText, "format",
                        "\\\\u00(08|09|0(a|A)|0(c|C)|0(d|D)|22|27|5(C|c))|\\\\(0(8|9|a|c|d)|1(0|1|2|3|4|5|6|7|8|9|a|b|c|d|e|f))");
                addProperty(doc, illegalTokenText, "message",
                        "Avoid using corresponding octal or Unicode escape sequences.");
                treeWalker.appendChild(illegalTokenText);
            }

            if (Boolean.TRUE.equals(dto.getAvoidEscapedUnicodeCharacters())) {
                Element avoidEscaped = doc.createElement("module");
                avoidEscaped.setAttribute("name", "AvoidEscapedUnicodeCharacters");
                addProperty(doc, avoidEscaped, "allowEscapesForControlCharacters", "true");
                addProperty(doc, avoidEscaped, "allowNonPrintableEscapes", "true");
                treeWalker.appendChild(avoidEscaped);
            }

            if (Boolean.TRUE.equals(dto.getAvoidStarImport())) {
                addModule(doc, treeWalker, "AvoidStarImport");
            }
            if (Boolean.TRUE.equals(dto.getOneTopLevelClass())) {
                addModule(doc, treeWalker, "OneTopLevelClass");
            }
            if (Boolean.TRUE.equals(dto.getNoLineWrap())) {
                addModule(doc, treeWalker, "NoLineWrap");
            }
            if (Boolean.TRUE.equals(dto.getEmptyBlock())) {
                addModule(doc, treeWalker, "EmptyBlock");
            }
            if (Boolean.TRUE.equals(dto.getNeedBraces())) {
                addModule(doc, treeWalker, "NeedBraces");
            }
            if (Boolean.TRUE.equals(dto.getLeftCurly())) {
                addModule(doc, treeWalker, "LeftCurly");
            }
            if (Boolean.TRUE.equals(dto.getRightCurly())) {
                addModule(doc, treeWalker, "RightCurly");
            }
            if (Boolean.TRUE.equals(dto.getEmptyStatement())) {
                addModule(doc, treeWalker, "EmptyStatement");
            }
            if (Boolean.TRUE.equals(dto.getEqualsHashCode())) {
                addModule(doc, treeWalker, "EqualsHashCode");
            }
            if (Boolean.TRUE.equals(dto.getIllegalInstantiation())) {
                addModule(doc, treeWalker, "IllegalInstantiation");
            }
            if (Boolean.TRUE.equals(dto.getMissingSwitchDefault())) {
                addModule(doc, treeWalker, "MissingSwitchDefault");
            }
            if (Boolean.TRUE.equals(dto.getSimplifyBooleanExpression())) {
                addModule(doc, treeWalker, "SimplifyBooleanExpression");
            }
            if (Boolean.TRUE.equals(dto.getSimplifyBooleanReturn())) {
                addModule(doc, treeWalker, "SimplifyBooleanReturn");
            }
            if (Boolean.TRUE.equals(dto.getFinalClass())) {
                addModule(doc, treeWalker, "FinalClass");
            }
            if (Boolean.TRUE.equals(dto.getHideUtilityClassConstructor())) {
                addModule(doc, treeWalker, "HideUtilityClassConstructor");
            }
            if (Boolean.TRUE.equals(dto.getInterfaceIsType())) {
                addModule(doc, treeWalker, "InterfaceIsType");
            }
            if (Boolean.TRUE.equals(dto.getVisibilityModifier())) {
                addModule(doc, treeWalker, "VisibilityModifier");
            }

            return documentToString(doc);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate XML configuration: " + e.getMessage(), e);
        }
    }

    private String getPropertyValue(Element parent, String propertyName, String defaultValue) {
        NodeList properties = parent.getElementsByTagName("property");
        for (int i = 0; i < properties.getLength(); i++) {
            Element property = (Element) properties.item(i);
            if (propertyName.equals(property.getAttribute("name"))) {
                return property.getAttribute("value");
            }
        }
        return defaultValue;
    }

    private Element findModuleByName(Element parent, String moduleName) {
        NodeList modules = parent.getElementsByTagName("module");
        for (int i = 0; i < modules.getLength(); i++) {
            Element module = (Element) modules.item(i);
            if (moduleName.equals(module.getAttribute("name"))) {
                return module;
            }
        }
        return null;
    }

    private boolean hasModule(Element parent, String moduleName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if ("module".equals(element.getTagName()) &&
                        moduleName.equals(element.getAttribute("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addProperty(Document doc, Element parent, String name, String value) {
        Element property = doc.createElement("property");
        property.setAttribute("name", name);
        property.setAttribute("value", value);
        parent.appendChild(property);
    }

    private void addModule(Document doc, Element parent, String moduleName) {
        Element module = doc.createElement("module");
        module.setAttribute("name", moduleName);
        parent.appendChild(module);
    }

    private String documentToString(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//Puppy Crawl//DTD Check Configuration 1.3//EN");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "https://checkstyle.org/dtds/configuration_1_3.dtd");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }
}

