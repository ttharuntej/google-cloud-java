package com.google.gcloud.datastore;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.api.services.datastore.DatastoreV1;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * A key that is guaranteed to be complete and could be used to reference a
 * Google Cloud Datastore {@link Entity}.
 * This class is immutable. To edit (a copy) use {@link #builder()}.
 *
 * @see <a href="https://cloud.google.com/datastore/docs/concepts/entities">Google Cloud Datastore Entities, Properties, and Keys</a>
 */
public final class Key extends PartialKey {

  private static final long serialVersionUID = 3160994559785491356L;

  public static final class Builder extends PartialKey.Builder {

    private String name;
    private Long id;

    public Builder(String dataset, String kind, String name) {
      super(dataset, kind);
      this.name = name;
    }

    public Builder(String dataset, String kind, long id) {
      super(dataset, kind);
      this.id = id;
    }

    public Builder(Key parent, String kind, String name) {
      super(parent, kind);
      this.name = name;
    }

    public Builder(Key parent, String kind, long id) {
      super(parent, kind);
      this.id = id;
    }

    @Override
    public Builder addAncestor(String kind, long id) {
      super.addAncestor(kind, id);
      return this;
    }

    @Override
    public Builder addAncestor(String kind, String name) {
      super.addAncestor(kind, name);
      return this;
    }

    @Override
    public Builder addAncestor(Ancestor... ancestor) {
      super.addAncestor(ancestor);
      return this;
    }

    @Override
    public Builder addAncestors(Iterable<Ancestor> ancestors) {
      super.addAncestors(ancestors);
      return this;
    }

    @Override
    public Builder kind(String kind) {
      super.kind(kind);
      return this;
    }

    @Override
    public Builder clearPath() {
      super.clearPath();
      return this;
    }

    @Override
    public Builder dataset(String dataset) {
      super.dataset(dataset);
      return this;
    }

    @Override
    public Builder namespace(String namespace) {
      super.namespace(namespace);
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      id = null;
      return this;
    }

    public Builder id(long id) {
      this.id = id;
      name = null;
      return this;
    }

    @Override
    public Key build() {
      PartialKey key = super.build();
      return id == null ? new Key(key, name) : new Key(key, id);
    }
  }

  private Key(PartialKey key, String name) {
    super(key.dataset(), key.namespace(), newPath(key, name));
  }

  private Key(PartialKey key, long id) {
    super(key.dataset(), key.namespace(), newPath(key, id));
  }

  @Override
  public Builder builder() {
    Builder builder =
        hasId() ? new Builder(dataset(), kind(), id()) : new Builder(dataset(), kind(), name());
    return builder.namespace(namespace()).addAncestors(ancestors());
  }

  public boolean hasId() {
    return id() != null;
  }

  /**
   * Returns the key's id or {@code null} if it has a name instead.
   */
  public Long id() {
    return getLeaf().id();
  }

  public boolean hasName() {
    return name() != null;
  }

  /**
   * Returns the key's name or {@code null} if it has an id instead.
   */
  public String name() {
    return getLeaf().name();
  }

  /**
   * Returns the key's id (as {@link #Long}) or name (as {@link String}).
   */
  public Object nameOrId() {
    Ancestor leaf = getLeaf();
    return leaf.hasId() ? leaf.id() : leaf.name();
  }

  /**
   * Returns the key in an encoded form that can be used as part of a URL.
   */
  public String toUrlSafe() {
    try {
      return URLEncoder.encode(toString(), UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Unxpeced encoding exception", e);
    }
  }

  /**
   * Create a {@code Key} given its URL safe encoded form.
   *
   * @throws RuntimeException when decoding fails
   */
  public static Key fromUrlSafe(String urlSafe) {
    try {
      String utf8Str = URLDecoder.decode(urlSafe, UTF_8.name());
      DatastoreV1.Key keyPb = DatastoreV1.Key.parseFrom(ByteString.copyFromUtf8(utf8Str));
      return Key.fromPb(keyPb);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Unxpeced decoding exception", e);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException("Could not parse key", e);
    }
  }

  /**
   * Convert an {@code IncompleteKey} to a {@code Key} provided that the key has
   * either name or id (complete).

   * @throws IllegalArgumentException if provided key is not complete.
   */
  public static Key fromIncompleteKey(PartialKey key) {
    if (key instanceof Key) {
      return (Key) key;
    }
    Ancestor leaf = key.getLeaf();
    if (leaf.hasId()) {
      return new Key(key, leaf.id());
    } else if (leaf.hasName()) {
      return new Key(key, leaf.name());
    }
    throw new IllegalArgumentException("Key is missing name or id");
  }

  @Override
  protected Object fromPb(byte[] bytesPb) throws InvalidProtocolBufferException {
    return fromPb(DatastoreV1.Key.parseFrom(bytesPb));
  }

  static Key fromPb(DatastoreV1.Key keyPb) {
    return fromIncompleteKey(PartialKey.fromPb(keyPb));
  }

  private static ImmutableList<Ancestor> newPath(PartialKey key, String name) {
    ImmutableList.Builder<Ancestor> path = ImmutableList.builder();
    path.addAll(key.ancestors());
    path.add(new Ancestor(key.kind(), name));
    return path.build();
  }

  private static ImmutableList<Ancestor> newPath(PartialKey key, long id) {
    ImmutableList.Builder<Ancestor> path = ImmutableList.builder();
    path.addAll(key.ancestors());
    path.add(new Ancestor(key.kind(), id));
    return path.build();
  }
}