package elide.grpctest;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * Hello service interface.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.51.0)",
    comments = "Source: nopackage.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class HelloServiceGrpc {

  private HelloServiceGrpc() {}

  public static final String SERVICE_NAME = "elide.grpctest.HelloService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<Nopackage.HelloRequest,
      Nopackage.HelloResponse> getRenderMessageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RenderMessage",
      requestType = Nopackage.HelloRequest.class,
      responseType = Nopackage.HelloResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Nopackage.HelloRequest,
      Nopackage.HelloResponse> getRenderMessageMethod() {
    io.grpc.MethodDescriptor<Nopackage.HelloRequest, Nopackage.HelloResponse> getRenderMessageMethod;
    if ((getRenderMessageMethod = HelloServiceGrpc.getRenderMessageMethod) == null) {
      synchronized (HelloServiceGrpc.class) {
        if ((getRenderMessageMethod = HelloServiceGrpc.getRenderMessageMethod) == null) {
          HelloServiceGrpc.getRenderMessageMethod = getRenderMessageMethod =
              io.grpc.MethodDescriptor.<Nopackage.HelloRequest, Nopackage.HelloResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RenderMessage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Nopackage.HelloRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Nopackage.HelloResponse.getDefaultInstance()))
              .setSchemaDescriptor(new HelloServiceMethodDescriptorSupplier("RenderMessage"))
              .build();
        }
      }
    }
    return getRenderMessageMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static HelloServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<HelloServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<HelloServiceStub>() {
        @Override
        public HelloServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new HelloServiceStub(channel, callOptions);
        }
      };
    return HelloServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static HelloServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<HelloServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<HelloServiceBlockingStub>() {
        @Override
        public HelloServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new HelloServiceBlockingStub(channel, callOptions);
        }
      };
    return HelloServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static HelloServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<HelloServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<HelloServiceFutureStub>() {
        @Override
        public HelloServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new HelloServiceFutureStub(channel, callOptions);
        }
      };
    return HelloServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * Hello service interface.
   * </pre>
   */
  public static abstract class HelloServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Method that renders a hello message with a given name.
     * </pre>
     */
    public void renderMessage(Nopackage.HelloRequest request,
                              io.grpc.stub.StreamObserver<Nopackage.HelloResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRenderMessageMethod(), responseObserver);
    }

    @Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getRenderMessageMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                Nopackage.HelloRequest,
                Nopackage.HelloResponse>(
                  this, METHODID_RENDER_MESSAGE)))
          .build();
    }
  }

  /**
   * <pre>
   * Hello service interface.
   * </pre>
   */
  public static final class HelloServiceStub extends io.grpc.stub.AbstractAsyncStub<HelloServiceStub> {
    private HelloServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected HelloServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new HelloServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Method that renders a hello message with a given name.
     * </pre>
     */
    public void renderMessage(Nopackage.HelloRequest request,
                              io.grpc.stub.StreamObserver<Nopackage.HelloResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRenderMessageMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * Hello service interface.
   * </pre>
   */
  public static final class HelloServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<HelloServiceBlockingStub> {
    private HelloServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected HelloServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new HelloServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Method that renders a hello message with a given name.
     * </pre>
     */
    public Nopackage.HelloResponse renderMessage(Nopackage.HelloRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRenderMessageMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * Hello service interface.
   * </pre>
   */
  public static final class HelloServiceFutureStub extends io.grpc.stub.AbstractFutureStub<HelloServiceFutureStub> {
    private HelloServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected HelloServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new HelloServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Method that renders a hello message with a given name.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Nopackage.HelloResponse> renderMessage(
        Nopackage.HelloRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRenderMessageMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_RENDER_MESSAGE = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final HelloServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(HelloServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_RENDER_MESSAGE:
          serviceImpl.renderMessage((Nopackage.HelloRequest) request,
              (io.grpc.stub.StreamObserver<Nopackage.HelloResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @Override
    @SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class HelloServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    HelloServiceBaseDescriptorSupplier() {}

    @Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return Nopackage.getDescriptor();
    }

    @Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("HelloService");
    }
  }

  private static final class HelloServiceFileDescriptorSupplier
      extends HelloServiceBaseDescriptorSupplier {
    HelloServiceFileDescriptorSupplier() {}
  }

  private static final class HelloServiceMethodDescriptorSupplier
      extends HelloServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    HelloServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (HelloServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new HelloServiceFileDescriptorSupplier())
              .addMethod(getRenderMessageMethod())
              .build();
        }
      }
    }
    return result;
  }
}
