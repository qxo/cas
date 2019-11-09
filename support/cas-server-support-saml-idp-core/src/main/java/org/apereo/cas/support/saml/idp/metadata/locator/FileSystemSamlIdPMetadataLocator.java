package org.apereo.cas.support.saml.idp.metadata.locator;

import org.apereo.cas.support.saml.services.SamlRegisteredService;
import org.apereo.cas.support.saml.services.idp.metadata.SamlIdPMetadataDocument;
import org.apereo.cas.util.crypto.CipherExecutor;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * This is {@link FileSystemSamlIdPMetadataLocator}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
@Slf4j
public class FileSystemSamlIdPMetadataLocator extends AbstractSamlIdPMetadataLocator {
    private final File metadataLocation;

    public FileSystemSamlIdPMetadataLocator(final Resource resource) throws Exception {
        this(resource.getFile());
    }

    public FileSystemSamlIdPMetadataLocator(final File resource) {
        super(CipherExecutor.noOpOfStringToString());
        this.metadataLocation = resource;
    }

    private static String getAppliesToFor(final Optional<SamlRegisteredService> result) {
        if (result.isPresent()) {
            val registeredService = result.get();
            return registeredService.getName() + '-' + registeredService.getId();
        }
        return "CAS";
    }

    @Override
    public Resource resolveSigningCertificate(final Optional<SamlRegisteredService> registeredService) {
        return new FileSystemResource(new File(determineMetadataLocationFor(registeredService), "/idp-signing.crt"));
    }

    @Override
    public Resource resolveSigningKey(final Optional<SamlRegisteredService> registeredService) {
        return new FileSystemResource(new File(determineMetadataLocationFor(registeredService), "/idp-signing.key"));
    }

    @Override
    public Resource resolveMetadata(final Optional<SamlRegisteredService> registeredService) {
        return new FileSystemResource(new File(determineMetadataLocationFor(registeredService), "idp-metadata.xml"));
    }

    @Override
    public Resource getEncryptionCertificate(final Optional<SamlRegisteredService> registeredService) {
        return new FileSystemResource(new File(determineMetadataLocationFor(registeredService), "/idp-encryption.crt"));
    }

    @Override
    public Resource resolveEncryptionKey(final Optional<SamlRegisteredService> registeredService) {
        return new FileSystemResource(new File(determineMetadataLocationFor(registeredService), "/idp-encryption.key"));
    }

    @Override
    public boolean exists(final Optional<SamlRegisteredService> registeredService) {
        return resolveMetadata(registeredService).exists();
    }

    @SneakyThrows
    @Override
    protected SamlIdPMetadataDocument fetchInternal(final Optional<SamlRegisteredService> registeredService) {
        val doc = new SamlIdPMetadataDocument();
        doc.setMetadata(IOUtils.toString(resolveMetadata(registeredService).getInputStream(), StandardCharsets.UTF_8));
        doc.setEncryptionCertificate(IOUtils.toString(getEncryptionCertificate(registeredService).getInputStream(), StandardCharsets.UTF_8));
        doc.setEncryptionKey(IOUtils.toString(resolveEncryptionKey(registeredService).getInputStream(), StandardCharsets.UTF_8));
        doc.setSigningCertificate(IOUtils.toString(resolveSigningCertificate(registeredService).getInputStream(), StandardCharsets.UTF_8));
        doc.setSigningKey(IOUtils.toString(resolveSigningKey(registeredService).getInputStream(), StandardCharsets.UTF_8));
        doc.setAppliesTo(getAppliesToFor(registeredService));
        return doc;
    }

    @Override
    public void initialize() {
        if (!this.metadataLocation.exists()) {
            LOGGER.debug("Metadata directory [{}] does not exist. Creating...", this.metadataLocation);
            if (!this.metadataLocation.mkdir()) {
                throw new IllegalArgumentException("Metadata directory location " + this.metadataLocation + " cannot be located/created");
            }
        }
        LOGGER.info("Metadata directory location is at [{}]", this.metadataLocation);
    }

    private File determineMetadataLocationFor(final Optional<SamlRegisteredService> result) {
        if (result.isEmpty()) {
            if (!this.metadataLocation.exists() && !this.metadataLocation.mkdirs()) {
                throw new IllegalArgumentException("Metadata directory location " + this.metadataLocation + " cannot be located/created");
            }
            return this.metadataLocation;
        }
        val serviceDirectory = new File(this.metadataLocation, getAppliesToFor(result));
        if (!serviceDirectory.exists() && !serviceDirectory.mkdirs()) {
            throw new IllegalArgumentException("Metadata directory location " + serviceDirectory + " cannot be located/created");
        }
        return serviceDirectory;
    }
}
