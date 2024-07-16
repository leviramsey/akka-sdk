/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.platform.javasdk;

import akka.actor.ActorSystem;
import akka.annotation.InternalApi;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import akka.platform.javasdk.action.Action;
import akka.platform.javasdk.action.ActionOptions;
import akka.platform.javasdk.action.ActionProvider;
import akka.platform.javasdk.eventsourcedentity.EventSourcedEntity;
import akka.platform.javasdk.eventsourcedentity.EventSourcedEntityOptions;
import akka.platform.javasdk.eventsourcedentity.EventSourcedEntityProvider;
import akka.platform.javasdk.impl.*;
import akka.platform.javasdk.impl.action.ActionService;
import akka.platform.javasdk.impl.action.ResolvedActionFactory;
import akka.platform.javasdk.impl.eventsourcedentity.EventSourcedEntityService;
import akka.platform.javasdk.impl.eventsourcedentity.ResolvedEventSourcedEntityFactory;
import akka.platform.javasdk.impl.keyvalueentity.ResolvedKeyValueEntityFactory;
import akka.platform.javasdk.impl.keyvalueentity.KeyValueEntityService;
import akka.platform.javasdk.impl.view.ViewService;
import akka.platform.javasdk.impl.workflow.ResolvedWorkflowFactory;
import akka.platform.javasdk.impl.workflow.WorkflowService;
import akka.platform.javasdk.keyvalueentity.KeyValueEntity;
import akka.platform.javasdk.keyvalueentity.KeyValueEntityOptions;
import akka.platform.javasdk.keyvalueentity.KeyValueEntityProvider;
import akka.platform.javasdk.view.ViewOptions;
import akka.platform.javasdk.view.ViewProvider;
import akka.platform.javasdk.workflow.AbstractWorkflow;
import akka.platform.javasdk.workflow.WorkflowOptions;
import akka.platform.javasdk.workflow.WorkflowProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

/**
 * The Kalix class is the main interface to configuring entities to deploy, and subsequently
 * starting a local server which will expose these entities to the Kalix Runtime Sidecar.
 */
public final class Kalix {

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final Map<String, Function<ActorSystem, Service>> services = new HashMap<>();
  private ClassLoader classLoader = getClass().getClassLoader();
  private String typeUrlPrefix = AnySupport.DefaultTypeUrlPrefix();
  private AnySupport.Prefer prefer = AnySupport.PREFER_JAVA();
  private final LowLevelRegistration lowLevel = new LowLevelRegistration();
  private String sdkName = BuildInfo$.MODULE$.name();

  private Set<Descriptors.FileDescriptor> allDescriptors = new HashSet<>();

  private DescriptorProtos.FileDescriptorProto aclDescriptor;

  private class LowLevelRegistration {
    /**
     * Register an event sourced entity factory.
     *
     * <p>This is a low level API intended for custom mechanisms for implementing the entity.
     *
     * @param factory               The event sourced factory.
     * @param descriptor            The descriptor for the service that this entity implements.
     * @param entityType            The persistence id for this entity.
     * @param entityOptions         the options for this entity.
     * @param additionalDescriptors Any additional descriptors that should be used to look up
     *                              protobuf types when needed.
     * @return This stateful service builder.
     */
    public Kalix registerEventSourcedEntity(
      EventSourcedEntityFactory factory,
      Descriptors.ServiceDescriptor descriptor,
      String entityType,
      EventSourcedEntityOptions entityOptions,
      Descriptors.FileDescriptor... additionalDescriptors) {

      AnySupport anySupport = newAnySupport(additionalDescriptors);
      EventSourcedEntityFactory resolvedFactory =
        new ResolvedEventSourcedEntityFactory(
          factory, anySupport.resolveServiceDescriptor(descriptor));

      return registerEventSourcedEntity(
        descriptor,
        entityType,
        entityOptions,
        anySupport,
        resolvedFactory,
        additionalDescriptors);
    }

    public Kalix registerEventSourcedEntity(
      Descriptors.ServiceDescriptor descriptor,
      String entityType,
      EventSourcedEntityOptions entityOptions,
      MessageCodec messageCodec,
      EventSourcedEntityFactory resolvedFactory,
      Descriptors.FileDescriptor[] additionalDescriptors) {
      services.put(
        descriptor.getFullName(),
        system ->
          new EventSourcedEntityService(
            resolvedFactory,
            descriptor,
            additionalDescriptors,
            messageCodec,
            entityType,
            entityOptions.snapshotEvery(),
            entityOptions));

      return Kalix.this;
    }


    public Kalix registerWorkflow(
      WorkflowFactory factory,
      Descriptors.ServiceDescriptor descriptor,
      String workflowName,
      WorkflowOptions workflowOptions,
      Descriptors.FileDescriptor... additionalDescriptors) {

      AnySupport anySupport = newAnySupport(additionalDescriptors);
      WorkflowFactory resolvedFactory =
        new ResolvedWorkflowFactory(factory, anySupport.resolveServiceDescriptor(descriptor));

      return registerWorkflow(descriptor,
        workflowName,
        workflowOptions,
        anySupport,
        resolvedFactory,
        additionalDescriptors
      );
    }


    public Kalix registerWorkflow(
      Descriptors.ServiceDescriptor descriptor,
      String workflowName,
      WorkflowOptions workflowOptions,
      MessageCodec messageCodec,
      WorkflowFactory resolvedFactory,
      Descriptors.FileDescriptor[] additionalDescriptors) {

      services.put(
        descriptor.getFullName(),
        system ->
          new WorkflowService(
            resolvedFactory,
            descriptor,
            additionalDescriptors,
            messageCodec,
            workflowName,
            workflowOptions
          )
      );

      return Kalix.this;
    }

    /**
     * Register an Action handler.
     *
     * <p>This is a low level API intended for custom mechanisms for implementing the action.
     *
     * @param descriptor            The descriptor for the service that this action implements.
     * @param additionalDescriptors Any additional descriptors that should be used to look up
     *                              protobuf types when needed.
     * @return This Kalix builder.
     */
    public Kalix registerAction(
      ActionFactory actionFactory,
      ActionOptions actionOptions,
      Descriptors.ServiceDescriptor descriptor,
      Descriptors.FileDescriptor... additionalDescriptors) {

      final AnySupport anySupport = newAnySupport(additionalDescriptors);
      ActionFactory resolvedActionFactory =
        new ResolvedActionFactory(actionFactory, anySupport.resolveServiceDescriptor(descriptor));

      return registerAction(
        resolvedActionFactory, anySupport, actionOptions, descriptor, additionalDescriptors);
    }

    public Kalix registerAction(
      ActionFactory actionFactory,
      MessageCodec messageCodec,
      ActionOptions actionOptions,
      Descriptors.ServiceDescriptor descriptor,
      Descriptors.FileDescriptor... additionalDescriptors) {

      ActionService service =
        new ActionService(
          actionFactory, descriptor, additionalDescriptors, messageCodec, actionOptions);

      services.put(descriptor.getFullName(), system -> service);

      return Kalix.this;
    }

    /**
     * Register a value based entity factory.
     *
     * <p>This is a low level API intended for custom mechanisms for implementing the entity.
     *
     * @param factory       The value based entity factory.
     * @param descriptor    The descriptor for the service that this entity implements.
     * @param typeId        The entity type name
     * @param entityOptions The options for this entity.
     * @return This stateful service builder.
     */
    public Kalix registerKeyValueEntity(
      KeyValueEntityFactory factory,
      Descriptors.ServiceDescriptor descriptor,
      String typeId,
      KeyValueEntityOptions entityOptions,
      Descriptors.FileDescriptor... additionalDescriptors) {

      AnySupport anySupport = newAnySupport(additionalDescriptors);
      KeyValueEntityFactory resolvedFactory =
        new ResolvedKeyValueEntityFactory(factory, anySupport.resolveServiceDescriptor(descriptor));

      return registerKeyValueEntity(
        resolvedFactory,
        anySupport,
        descriptor,
        typeId,
        entityOptions,
        additionalDescriptors);
    }

    public Kalix registerKeyValueEntity(
      KeyValueEntityFactory factory,
      MessageCodec messageCodec,
      Descriptors.ServiceDescriptor descriptor,
      String entityType,
      KeyValueEntityOptions entityOptions,
      Descriptors.FileDescriptor... additionalDescriptors) {

      KeyValueEntityService service =
        new KeyValueEntityService(
          factory, descriptor, additionalDescriptors, messageCodec, entityType, entityOptions);

      services.put(descriptor.getFullName(), system -> service);

      return Kalix.this;
    }


    /**
     * Register a view factory.
     *
     * <p>This is a low level API intended for custom mechanisms for implementing the view.
     *
     * @param factory               The view factory.
     * @param descriptor            The descriptor for the service that this entity implements.
     * @param viewId                The id of this view, used for persistence.
     * @param additionalDescriptors Any additional descriptors that should be used to look up
     *                              protobuf types when needed.
     * @return This stateful service builder.
     */
    private Kalix registerView(
      ViewFactory factory,
      Descriptors.ServiceDescriptor descriptor,
      String viewId,
      ViewOptions viewOptions,
      Descriptors.FileDescriptor... additionalDescriptors) {

      AnySupport anySupport = newAnySupport(additionalDescriptors);
      return registerView(
        factory, anySupport, descriptor, viewId, viewOptions, additionalDescriptors);
    }

    private Kalix registerView(
      ViewFactory factory,
      MessageCodec messageCodec,
      Descriptors.ServiceDescriptor descriptor,
      String viewId,
      ViewOptions viewOptions,
      Descriptors.FileDescriptor... additionalDescriptors) {

      ViewService service =
        new ViewService(
          Optional.ofNullable(factory),
          descriptor,
          additionalDescriptors,
          messageCodec,
          viewId,
          viewOptions);
      services.put(descriptor.getFullName(), system -> service);

      return Kalix.this;
    }
  }

  /**
   * Sets the ClassLoader to be used for reflective access, the default value is the ClassLoader of
   * the Kalix class.
   *
   * @param classLoader A non-null ClassLoader to be used for reflective access.
   * @return This Kalix instance.
   */
  public Kalix withClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
    return this;
  }

  /**
   * Sets the type URL prefix to be used when serializing and deserializing types from and to
   * Protobuf Any values. Defaults to "type.googleapis.com".
   *
   * @param prefix the type URL prefix to be used.
   * @return This Kalix instance.
   */
  public Kalix withTypeUrlPrefix(String prefix) {
    this.typeUrlPrefix = prefix;
    return this;
  }

  /**
   * When locating protobufs, if both a Java and a ScalaPB generated class is found on the
   * classpath, this specifies that Java should be preferred.
   *
   * @return This Kalix instance.
   */
  public Kalix preferJavaProtobufs() {
    this.prefer = AnySupport.PREFER_JAVA();
    return this;
  }

  /**
   * When locating protobufs, if both a Java and a ScalaPB generated class is found on the
   * classpath, this specifies that Scala should be preferred.
   *
   * @return This Kalix instance.
   */
  public Kalix preferScalaProtobufs() {
    this.prefer = AnySupport.PREFER_SCALA();
    return this;
  }

  /**
   * INTERNAL API - subject to change without notice
   *
   * @param sdkName the name of the SDK used to build this Kalix app (i.e. kalix-java-sdk)
   * @return This Kalix instance.
   */
  public Kalix withSdkName(String sdkName) {
    this.sdkName = sdkName;
    return this;
  }


  /**
   * INTERNAL API - subject to change without notice
   *
   * @param aclDescriptor - the default ACL file descriptor
   * @return This Kalix instance.
   */
  public Kalix withDefaultAclFileDescriptor(DescriptorProtos.FileDescriptorProto aclDescriptor) {
    this.aclDescriptor = aclDescriptor;
    return this;
  }



  /**
   * Register a value based entity using a {{@link KeyValueEntityProvider}}. The concrete <code>
   * KeyValueEntityProvider</code> is generated for the specific entities defined in Protobuf, for
   * example <code>CustomerEntityProvider</code>.
   *
   * <p>{{@link KeyValueEntityOptions}} can be defined by in the <code>ValueEntityProvider</code>.
   *
   * @return This stateful service builder.
   */
  public <S, E extends KeyValueEntity<S>> Kalix register(KeyValueEntityProvider<S, E> provider) {
    return provider
      .alternativeCodec()
      .map(
        codec ->
          lowLevel.registerKeyValueEntity(
            provider::newRouter,
            codec,
            provider.serviceDescriptor(),
            provider.typeId(),
            provider.options(),
            provider.additionalDescriptors()))
      .orElseGet(
        () ->
          lowLevel.registerKeyValueEntity(
            provider::newRouter,
            provider.serviceDescriptor(),
            provider.typeId(),
            provider.options(),
            provider.additionalDescriptors()));
  }

  /**
   * Register an event sourced entity using a {{@link EventSourcedEntityProvider}}. The concrete
   * <code>
   * EventSourcedEntityProvider</code> is generated for the specific entities defined in Protobuf,
   * for example <code>CustomerEntityProvider</code>.
   *
   * <p>{{@link EventSourcedEntityOptions}} can be defined by in the <code>
   * EventSourcedEntityProvider</code>.
   *
   * @return This stateful service builder.
   */
  public <S, E, ES extends EventSourcedEntity<S, E>> Kalix register(
      EventSourcedEntityProvider<S, E, ES> provider) {
    return provider
      .alternativeCodec()
      .map(
        codec ->
          lowLevel.registerEventSourcedEntity(
            provider.serviceDescriptor(),
            provider.typeId(),
            provider.options(),
            codec,
            provider::newRouter,
            provider.additionalDescriptors()))
      .orElseGet(
        () ->
          lowLevel.registerEventSourcedEntity(
            provider::newRouter,
            provider.serviceDescriptor(),
            provider.typeId(),
            provider.options(),
            provider.additionalDescriptors()));
  }


  public <S, W extends AbstractWorkflow<S>> Kalix register(WorkflowProvider<S, W> provider) {
    return provider
      .alternativeCodec()
      .map(
        codec ->
          lowLevel.registerWorkflow(
            provider.serviceDescriptor(),
            provider.typeId(),
            provider.options(),
            codec,
            provider::newRouter,
            provider.additionalDescriptors()
          )
      ).orElseGet(
        () ->
          lowLevel.registerWorkflow(
            provider::newRouter,
            provider.serviceDescriptor(),
            provider.typeId(),
            provider.options(),
            provider.additionalDescriptors()
          )
      );
  }

  /**
   * Register a view using a {@link ViewProvider}. The concrete <code>
   * ViewProvider</code> is generated for the specific views defined in Protobuf, for example <code>
   * CustomerViewProvider</code>.
   *
   * @return This stateful service builder.
   */
  public Kalix register(ViewProvider provider) {
    return provider
      .alternativeCodec()
      .map(
        codec ->
          lowLevel.registerView(
            provider::newRouter,
            codec,
            provider.serviceDescriptor(),
            provider.viewId(),
            provider.options(),
            provider.additionalDescriptors()))
      .orElseGet(
        () ->
          lowLevel.registerView(
            provider::newRouter,
            provider.serviceDescriptor(),
            provider.viewId(),
            provider.options(),
            provider.additionalDescriptors()));
  }

  /**
   * Register an action using an {{@link ActionProvider}}. The concrete <code>
   * ActionProvider</code> is generated for the specific entities defined in Protobuf, for example
   * <code>CustomerActionProvider</code>.
   *
   * @return This stateful service builder.
   */
  public <A extends Action> Kalix register(ActionProvider<A> provider) {
    return provider
      .alternativeCodec()
      .map(
        codec ->
          lowLevel.registerAction(
            provider::newRouter,
            codec,
            provider.options(),
            provider.serviceDescriptor(),
            provider.additionalDescriptors()))
      .orElseGet(
        () ->
          lowLevel.registerAction(
            provider::newRouter,
            provider.options(),
            provider.serviceDescriptor(),
            provider.additionalDescriptors()));
  }

  private AnySupport newAnySupport(Descriptors.FileDescriptor[] descriptors) {
    // we are interested in accumulating all descriptors from all registered components for later use in eventing testkit
    allDescriptors.addAll(Arrays.asList(descriptors));
    return new AnySupport(descriptors, classLoader, typeUrlPrefix, prefer);
  }

  /**
   * INTERNAL API
   * The returned codec includes all registered descriptors and is meant to be used internally for eventing testkit.
   */
  @InternalApi
  public MessageCodec getMessageCodec() {
    return new AnySupport(allDescriptors.toArray(new Descriptors.FileDescriptor[0]), classLoader, typeUrlPrefix, prefer);
  }

  /**
   * INTERNAL API
   */
  @InternalApi
  public Map<String, Function<ActorSystem, Service>> internalGetServices() {
    return services;
  }

}