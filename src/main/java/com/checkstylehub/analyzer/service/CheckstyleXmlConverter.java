package com.checkstylehub.analyzer.service;

import com.checkstylehub.analyzer.dto.CheckstyleRulesDto;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.xml.sax.SAXException;

/**
 * Service for converting between Checkstyle XML configuration and structured CheckstyleRulesDto.
 * Handles parsing XML to DTO and generating XML from DTO.
 */
@Service
public class CheckstyleXmlConverter {

    private static final String EL_MODULE = "module";
    private static final String EL_PROPERTY = "property";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_VALUE = "value";
    private static final String MODULE_CHECKER = "Checker";
    private static final String MODULE_TREE_WALKER = "TreeWalker";
    private static final String MODULE_LINE_LENGTH = "LineLength";
    private static final String MODULE_ILLEGAL_TOKEN_TEXT = "IllegalTokenText";
    private static final String MODULE_AVOID_ESCAPED_UNICODE = "AvoidEscapedUnicodeCharacters";
    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final String DEFAULT_SEVERITY = "warning";
    private static final String DEFAULT_FILE_EXTENSIONS = "java, properties, xml";
    private static final String PROP_CHARSET = "charset";
    private static final String PROP_SEVERITY = "severity";
    private static final String PROP_FILE_EXTENSIONS = "fileExtensions";
    private static final String PROP_MAX = "max";
    private static final String PROP_IGNORE_PATTERN = "ignorePattern";
    private static final String PROP_TOKENS = "tokens";
    private static final String PROP_FORMAT = "format";
    private static final String PROP_MESSAGE = "message";
    private static final String PROP_ALLOW_ESCAPES_CONTROL = "allowEscapesForControlCharacters";
    private static final String PROP_ALLOW_NON_PRINTABLE = "allowNonPrintableEscapes";
    private static final String VAL_STRING_CHAR_LITERALS = "STRING_LITERAL, CHAR_LITERAL";
    private static final String VAL_ILLEGAL_TOKEN_FORMAT = "\\\\u00(08|09|0(a|A)|0(c|C)|0(d|D)|22|27|5(C|c))|"
            + "\\\\(0(8|9|a|c|d)|1(0|1|2|3|4|5|6|7|8|9|a|b|c|d|e|f))";
    private static final String VAL_ILLEGAL_TOKEN_MESSAGE = "Avoid using corresponding octal or Unicode escape sequences.";
    private static final String VAL_TRUE = "true";
    private static final String DEFAULT_LINE_MAX = "120";
    private static final String INDENT_AMOUNT_KEY = "{https://xml.apache.org/xslt}indent-amount";
    private static final String INDENT_AMOUNT = "4";
    private static final String DOCTYPE_PUBLIC = "-//Puppy Crawl//DTD Check Configuration 1.3//EN";
    private static final String DOCTYPE_SYSTEM = "https://checkstyle.org/dtds/configuration_1_3.dtd";

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

            dto.setCharset(getPropertyValue(root, PROP_CHARSET, DEFAULT_CHARSET));
            dto.setSeverity(getPropertyValue(root, PROP_SEVERITY, DEFAULT_SEVERITY));
            dto.setFileExtensions(getPropertyValue(root, PROP_FILE_EXTENSIONS, DEFAULT_FILE_EXTENSIONS));

            Element lineLength = findModuleByName(root, MODULE_LINE_LENGTH);
            if (lineLength != null) {
                String maxValue = getPropertyValue(lineLength, PROP_MAX, DEFAULT_LINE_MAX);
                dto.setLineLength(Integer.parseInt(maxValue));
                dto.setLineLengthIgnorePattern(getPropertyValue(lineLength, PROP_IGNORE_PATTERN, ""));
            }

            Element treeWalker = findModuleByName(root, MODULE_TREE_WALKER);
            if (treeWalker != null) {
                applyTreeWalkerFlagsFromXml(treeWalker, dto);
            }

        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException("Failed to parse XML configuration: " + e.getMessage(), e);
        }

        return dto;
    }

    private void applyTreeWalkerFlagsFromXml(Element treeWalker, CheckstyleRulesDto dto) {
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

            Element checker = doc.createElement(EL_MODULE);
            checker.setAttribute(ATTR_NAME, MODULE_CHECKER);
            doc.appendChild(checker);

            addProperty(doc, checker, PROP_CHARSET, dto.getCharset());
            addProperty(doc, checker, PROP_SEVERITY, dto.getSeverity());
            addProperty(doc, checker, PROP_FILE_EXTENSIONS, dto.getFileExtensions());

            appendLineLengthModuleIfPresent(doc, checker, dto);

            Element treeWalker = doc.createElement(EL_MODULE);
            treeWalker.setAttribute(ATTR_NAME, MODULE_TREE_WALKER);
            checker.appendChild(treeWalker);

            appendTreeWalkerModulesFromDto(doc, treeWalker, dto);

            return documentToString(doc);

        } catch (ParserConfigurationException | TransformerException e) {
            throw new IllegalStateException("Failed to generate XML configuration: " + e.getMessage(), e);
        }
    }

    private void appendLineLengthModuleIfPresent(Document doc, Element checker, CheckstyleRulesDto dto) {
        if (dto.getLineLength() == null) {
            return;
        }
        Element lineLength = doc.createElement(EL_MODULE);
        lineLength.setAttribute(ATTR_NAME, MODULE_LINE_LENGTH);
        addProperty(doc, lineLength, PROP_MAX, String.valueOf(dto.getLineLength()));
        if (dto.getLineLengthIgnorePattern() != null && !dto.getLineLengthIgnorePattern().isEmpty()) {
            addProperty(doc, lineLength, PROP_IGNORE_PATTERN, dto.getLineLengthIgnorePattern());
        }
        checker.appendChild(lineLength);
    }

    private void appendTreeWalkerModulesFromDto(Document doc, Element treeWalker, CheckstyleRulesDto dto) {
        if (Boolean.TRUE.equals(dto.getOuterTypeFilename())) {
            addModule(doc, treeWalker, "OuterTypeFilename");
        }

        if (Boolean.TRUE.equals(dto.getIllegalTokenText())) {
            appendIllegalTokenTextModule(doc, treeWalker);
        }

        if (Boolean.TRUE.equals(dto.getAvoidEscapedUnicodeCharacters())) {
            appendAvoidEscapedUnicodeModule(doc, treeWalker);
        }

        addModuleIfTrue(doc, treeWalker, dto.getAvoidStarImport(), "AvoidStarImport");
        addModuleIfTrue(doc, treeWalker, dto.getOneTopLevelClass(), "OneTopLevelClass");
        addModuleIfTrue(doc, treeWalker, dto.getNoLineWrap(), "NoLineWrap");
        addModuleIfTrue(doc, treeWalker, dto.getEmptyBlock(), "EmptyBlock");
        addModuleIfTrue(doc, treeWalker, dto.getNeedBraces(), "NeedBraces");
        addModuleIfTrue(doc, treeWalker, dto.getLeftCurly(), "LeftCurly");
        addModuleIfTrue(doc, treeWalker, dto.getRightCurly(), "RightCurly");
        addModuleIfTrue(doc, treeWalker, dto.getEmptyStatement(), "EmptyStatement");
        addModuleIfTrue(doc, treeWalker, dto.getEqualsHashCode(), "EqualsHashCode");
        addModuleIfTrue(doc, treeWalker, dto.getIllegalInstantiation(), "IllegalInstantiation");
        addModuleIfTrue(doc, treeWalker, dto.getMissingSwitchDefault(), "MissingSwitchDefault");
        addModuleIfTrue(doc, treeWalker, dto.getSimplifyBooleanExpression(), "SimplifyBooleanExpression");
        addModuleIfTrue(doc, treeWalker, dto.getSimplifyBooleanReturn(), "SimplifyBooleanReturn");
        addModuleIfTrue(doc, treeWalker, dto.getFinalClass(), "FinalClass");
        addModuleIfTrue(doc, treeWalker, dto.getHideUtilityClassConstructor(), "HideUtilityClassConstructor");
        addModuleIfTrue(doc, treeWalker, dto.getInterfaceIsType(), "InterfaceIsType");
        addModuleIfTrue(doc, treeWalker, dto.getVisibilityModifier(), "VisibilityModifier");
    }

    private void appendIllegalTokenTextModule(Document doc, Element treeWalker) {
        Element illegalTokenText = doc.createElement(EL_MODULE);
        illegalTokenText.setAttribute(ATTR_NAME, MODULE_ILLEGAL_TOKEN_TEXT);
        addProperty(doc, illegalTokenText, PROP_TOKENS, VAL_STRING_CHAR_LITERALS);
        addProperty(doc, illegalTokenText, PROP_FORMAT, VAL_ILLEGAL_TOKEN_FORMAT);
        addProperty(doc, illegalTokenText, PROP_MESSAGE, VAL_ILLEGAL_TOKEN_MESSAGE);
        treeWalker.appendChild(illegalTokenText);
    }

    private void appendAvoidEscapedUnicodeModule(Document doc, Element treeWalker) {
        Element avoidEscaped = doc.createElement(EL_MODULE);
        avoidEscaped.setAttribute(ATTR_NAME, MODULE_AVOID_ESCAPED_UNICODE);
        addProperty(doc, avoidEscaped, PROP_ALLOW_ESCAPES_CONTROL, VAL_TRUE);
        addProperty(doc, avoidEscaped, PROP_ALLOW_NON_PRINTABLE, VAL_TRUE);
        treeWalker.appendChild(avoidEscaped);
    }

    private void addModuleIfTrue(Document doc, Element treeWalker, Boolean flag, String moduleName) {
        if (Boolean.TRUE.equals(flag)) {
            addModule(doc, treeWalker, moduleName);
        }
    }

    private String getPropertyValue(Element parent, String propertyName, String defaultValue) {
        NodeList properties = parent.getElementsByTagName(EL_PROPERTY);
        for (int i = 0; i < properties.getLength(); i++) {
            Element property = (Element) properties.item(i);
            if (propertyName.equals(property.getAttribute(ATTR_NAME))) {
                return property.getAttribute(ATTR_VALUE);
            }
        }
        return defaultValue;
    }

    private Element findModuleByName(Element parent, String moduleName) {
        NodeList modules = parent.getElementsByTagName(EL_MODULE);
        for (int i = 0; i < modules.getLength(); i++) {
            Element module = (Element) modules.item(i);
            if (moduleName.equals(module.getAttribute(ATTR_NAME))) {
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
                if (EL_MODULE.equals(element.getTagName())
                        && moduleName.equals(element.getAttribute(ATTR_NAME))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addProperty(Document doc, Element parent, String name, String value) {
        Element property = doc.createElement(EL_PROPERTY);
        property.setAttribute(ATTR_NAME, name);
        property.setAttribute(ATTR_VALUE, value);
        parent.appendChild(property);
    }

    private void addModule(Document doc, Element parent, String moduleName) {
        Element module = doc.createElement(EL_MODULE);
        module.setAttribute(ATTR_NAME, moduleName);
        parent.appendChild(module);
    }

    private String documentToString(Document doc) throws TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, DEFAULT_CHARSET);
        transformer.setOutputProperty(INDENT_AMOUNT_KEY, INDENT_AMOUNT);
        transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, DOCTYPE_PUBLIC);
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, DOCTYPE_SYSTEM);

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }
}
