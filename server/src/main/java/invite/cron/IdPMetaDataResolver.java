package invite.cron;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

@Component
public class IdPMetaDataResolver {

    private static final Log LOG = LogFactory.getLog(IdPMetaDataResolver.class);
    private static final List<String> languages = List.of("nl", "en");

    private final Resource metaDataResource;
    private Map<String, IdentityProvider> identityProviderMap = new HashMap<>();

    @Autowired
    public IdPMetaDataResolver(@Value("${cron.metadata-resolver-url}") Resource metaDataResource) {
        this.metaDataResource = metaDataResource;
    }

    @Scheduled(initialDelayString = "${cron.metadata-resolver-initial-delay-milliseconds}",
            fixedRateString = "${cron.metadata-resolver-fixed-rate-milliseconds}")
    public void resolveIdpMetaData() {
        long start = System.currentTimeMillis();
        Map<String, IdentityProvider> newIdentityProviderMap = new HashMap<>();
        String displayNameEn = null;
        String displayNameNl = null;
        String logoUrl = null;
        List<String> domainNames = new ArrayList<>();
        try {
            XMLStreamReader reader = getXMLStreamReader(metaDataResource);
            while (reader.hasNext()) {
                int next = reader.next();
                switch (next) {
                    case START_ELEMENT:
                        String localName = reader.getLocalName();
                        switch (localName) {
                            case "IDPSSODescriptor":
                                domainNames.clear();
                                displayNameEn = null;
                                displayNameNl = null;
                                logoUrl = null;
                                break;
                            case "Scope":
                                String scopeText = reader.getElementText();
                                if (StringUtils.hasText(scopeText)) {
                                    String domainName = scopeText.trim().replaceAll("\\n\\t ", "").toLowerCase();
                                    domainNames.add(domainName);
                                }
                                break;
                            case "DisplayName":
                                String lang = this.getAttributeValue(reader, "lang");
                                String displayName = reader.getElementText();
                                if (languages.contains(lang)) {
                                    if ("en".equals(lang)) {
                                        displayNameEn = displayName;
                                    } else {
                                        displayNameNl = displayName;
                                    }
                                }
                                break;
                            case "Logo":
                                logoUrl = reader.getElementText();
                                break;
                        }
                        break;
                    case END_ELEMENT:
                        localName = reader.getLocalName();
                        if (localName.equals("IDPSSODescriptor")) {
                            IdentityProvider identityProvider = new IdentityProvider(displayNameEn, displayNameNl, logoUrl);
                            for (String domainName : domainNames) {
                                newIdentityProviderMap.put(domainName, identityProvider);
                            }
                        }
                        break;
                }
            }
            this.identityProviderMap = newIdentityProviderMap;
            LOG.info(String.format("Parsed %s institution domain names from %s in %s ms",
                    identityProviderMap.size(), metaDataResource.getDescription(), System.currentTimeMillis() - start));
        } catch (FileNotFoundException fe) {
            LOG.warn(String.format("Could not read IdP Metadata from %s", metaDataResource.getDescription()));
        } catch (Exception e) {
            LOG.error("Error in resolveIdpMetaData", e);
        }
    }

    private String getAttributeValue(XMLStreamReader reader, String attributeName) {
        int attributeCount = reader.getAttributeCount();
        for (int i = 0; i < attributeCount; i++) {
            QName qName = reader.getAttributeName(i);
            if (qName.getLocalPart().equalsIgnoreCase(attributeName)) {
                return reader.getAttributeValue(qName.getNamespaceURI(), qName.getLocalPart());
            }
        }
        throw new IllegalArgumentException(String.format("Attribute %s is not present", attributeName));
    }


    public Optional<IdentityProvider> getIdentityProvider(String domainName) {
        lazyLoadCheck();
        return Optional.ofNullable(this.identityProviderMap.get(domainName));
    }

    private void lazyLoadCheck() {
        if (this.identityProviderMap.isEmpty()) {
            resolveIdpMetaData();
        }
    }

    private XMLStreamReader getXMLStreamReader(Resource xml) throws IOException, XMLStreamException {
        //despite it's name, the XMLInputFactoryImpl is not thread safe
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false); // This disables DTDs entirely for that factory
        xmlInputFactory.setProperty("javax.xml.stream.isSupportingExternalEntities", false); // disable external entities
        return xmlInputFactory.createXMLStreamReader(xml.getInputStream());
    }

}
