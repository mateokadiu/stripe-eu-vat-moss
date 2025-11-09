package io.github.mateokadiu.moss.file.saf;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.SAXException;

/**
 * Marshals a {@link SafOss} aggregate to UTF-8 bytes. When an XSD is registered via {@link
 * #withXsd}, the output is validated before being returned; an invalid document never leaves the
 * marshaller.
 */
public final class SafOssMarshaller {

  private static final String SCHEMA_LANG = "http://www.w3.org/2001/XMLSchema";

  private final Optional<Schema> schema;

  public SafOssMarshaller() {
    this(Optional.empty());
  }

  private SafOssMarshaller(Optional<Schema> schema) {
    this.schema = schema;
  }

  /** Returns a marshaller whose output is XSD-validated against the supplied schema. */
  public static SafOssMarshaller withXsd(byte[] xsd) {
    try {
      SchemaFactory factory = SchemaFactory.newInstance(SCHEMA_LANG);
      Schema s = factory.newSchema(new StreamSource(new java.io.ByteArrayInputStream(xsd)));
      return new SafOssMarshaller(Optional.of(s));
    } catch (SAXException ex) {
      throw new IllegalArgumentException("invalid XSD", ex);
    }
  }

  public MarshalResult marshal(SafOss doc) {
    try {
      JAXBContext ctx = JAXBContext.newInstance(SafOss.class);
      Marshaller m = ctx.createMarshaller();
      m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      schema.ifPresent(m::setSchema);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      m.marshal(doc, out);
      byte[] bytes = out.toByteArray();
      return new MarshalResult(bytes, sha256Hex(bytes));
    } catch (JAXBException ex) {
      throw new XmlValidationException("could not marshal SAF-OSS", ex);
    }
  }

  static String sha256Hex(byte[] bytes) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(md.digest(bytes));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  /** A successful marshal result — bytes + sha-256 hash for "we already filed this" detection. */
  public record MarshalResult(byte[] xml, String sha256) {

    public String xmlAsString() {
      return new String(xml, StandardCharsets.UTF_8);
    }
  }

  /** Raised when XSD validation rejects the marshalled bytes. */
  public static final class XmlValidationException extends RuntimeException {
    public XmlValidationException(String msg, Throwable cause) {
      super(msg, cause);
    }
  }
}
