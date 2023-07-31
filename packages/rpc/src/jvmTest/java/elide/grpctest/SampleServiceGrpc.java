package elide.grpctest;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * Sample service interface.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.51.0)",
    comments = "Source: sample.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class SampleServiceGrpc {

  private SampleServiceGrpc() {}

  public static final String SERVICE_NAME = "elide.grpctest.SampleService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<Sample.SampleRequest,
      Sample.SampleResponse> getMethodThatTakesTooLongMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "MethodThatTakesTooLong",
      requestType = Sample.SampleRequest.class,
      responseType = Sample.SampleResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Sample.SampleRequest,
      Sample.SampleResponse> getMethodThatTakesTooLongMethod() {
    io.grpc.MethodDescriptor<Sample.SampleRequest, Sample.SampleResponse> getMethodThatTakesTooLongMethod;
    if ((getMethodThatTakesTooLongMethod = SampleServiceGrpc.getMethodThatTakesTooLongMethod) == null) {
      synchronized (SampleServiceGrpc.class) {
        if ((getMethodThatTakesTooLongMethod = SampleServiceGrpc.getMethodThatTakesTooLongMethod) == null) {
          SampleServiceGrpc.getMethodThatTakesTooLongMethod = getMethodThatTakesTooLongMethod =
              io.grpc.MethodDescriptor.<Sample.SampleRequest, Sample.SampleResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "MethodThatTakesTooLong"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Sample.SampleRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Sample.SampleResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SampleServiceMethodDescriptorSupplier("MethodThatTakesTooLong"))
              .build();
        }
      }
    }
    return getMethodThatTakesTooLongMethod;
  }

  private static volatile io.grpc.MethodDescriptor<Sample.StatusRequest,
      Sample.SampleResponse> getMethodThatErrorsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "MethodThatErrors",
      requestType = Sample.StatusRequest.class,
      responseType = Sample.SampleResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Sample.StatusRequest,
      Sample.SampleResponse> getMethodThatErrorsMethod() {
    io.grpc.MethodDescriptor<Sample.StatusRequest, Sample.SampleResponse> getMethodThatErrorsMethod;
    if ((getMethodThatErrorsMethod = SampleServiceGrpc.getMethodThatErrorsMethod) == null) {
      synchronized (SampleServiceGrpc.class) {
        if ((getMethodThatErrorsMethod = SampleServiceGrpc.getMethodThatErrorsMethod) == null) {
          SampleServiceGrpc.getMethodThatErrorsMethod = getMethodThatErrorsMethod =
              io.grpc.MethodDescriptor.<Sample.StatusRequest, Sample.SampleResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "MethodThatErrors"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Sample.StatusRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Sample.SampleResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SampleServiceMethodDescriptorSupplier("MethodThatErrors"))
              .build();
        }
      }
    }
    return getMethodThatErrorsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<Sample.SampleRequest,
      Sample.SampleResponse> getMethodWithTrailersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "MethodWithTrailers",
      requestType = Sample.SampleRequest.class,
      responseType = Sample.SampleResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Sample.SampleRequest,
      Sample.SampleResponse> getMethodWithTrailersMethod() {
    io.grpc.MethodDescriptor<Sample.SampleRequest, Sample.SampleResponse> getMethodWithTrailersMethod;
    if ((getMethodWithTrailersMethod = SampleServiceGrpc.getMethodWithTrailersMethod) == null) {
      synchronized (SampleServiceGrpc.class) {
        if ((getMethodWithTrailersMethod = SampleServiceGrpc.getMethodWithTrailersMethod) == null) {
          SampleServiceGrpc.getMethodWithTrailersMethod = getMethodWithTrailersMethod =
              io.grpc.MethodDescriptor.<Sample.SampleRequest, Sample.SampleResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "MethodWithTrailers"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Sample.SampleRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Sample.SampleResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SampleServiceMethodDescriptorSupplier("MethodWithTrailers"))
              .build();
        }
      }
    }
    return getMethodWithTrailersMethod;
  }

  private static volatile io.grpc.MethodDescriptor<Sample.SampleRequest,
      Sample.SampleResponse> getMethodWithFatalErrorMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "MethodWithFatalError",
      requestType = Sample.SampleRequest.class,
      responseType = Sample.SampleResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Sample.SampleRequest,
      Sample.SampleResponse> getMethodWithFatalErrorMethod() {
    io.grpc.MethodDescriptor<Sample.SampleRequest, Sample.SampleResponse> getMethodWithFatalErrorMethod;
    if ((getMethodWithFatalErrorMethod = SampleServiceGrpc.getMethodWithFatalErrorMethod) == null) {
      synchronized (SampleServiceGrpc.class) {
        if ((getMethodWithFatalErrorMethod = SampleServiceGrpc.getMethodWithFatalErrorMethod) == null) {
          SampleServiceGrpc.getMethodWithFatalErrorMethod = getMethodWithFatalErrorMethod =
              io.grpc.MethodDescriptor.<Sample.SampleRequest, Sample.SampleResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "MethodWithFatalError"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Sample.SampleRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Sample.SampleResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SampleServiceMethodDescriptorSupplier("MethodWithFatalError"))
              .build();
        }
      }
    }
    return getMethodWithFatalErrorMethod;
  }

  private static volatile io.grpc.MethodDescriptor<Sample.SampleRequest,
      Sample.SampleResponse> getMethodWithMultipleResponsesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "MethodWithMultipleResponses",
      requestType = Sample.SampleRequest.class,
      responseType = Sample.SampleResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Sample.SampleRequest,
      Sample.SampleResponse> getMethodWithMultipleResponsesMethod() {
    io.grpc.MethodDescriptor<Sample.SampleRequest, Sample.SampleResponse> getMethodWithMultipleResponsesMethod;
    if ((getMethodWithMultipleResponsesMethod = SampleServiceGrpc.getMethodWithMultipleResponsesMethod) == null) {
      synchronized (SampleServiceGrpc.class) {
        if ((getMethodWithMultipleResponsesMethod = SampleServiceGrpc.getMethodWithMultipleResponsesMethod) == null) {
          SampleServiceGrpc.getMethodWithMultipleResponsesMethod = getMethodWithMultipleResponsesMethod =
              io.grpc.MethodDescriptor.<Sample.SampleRequest, Sample.SampleResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "MethodWithMultipleResponses"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Sample.SampleRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Sample.SampleResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SampleServiceMethodDescriptorSupplier("MethodWithMultipleResponses"))
              .build();
        }
      }
    }
    return getMethodWithMultipleResponsesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<Sample.SampleRequest,
      Sample.SampleResponse> getMethodWithNextAfterCloseMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "MethodWithNextAfterClose",
      requestType = Sample.SampleRequest.class,
      responseType = Sample.SampleResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Sample.SampleRequest,
      Sample.SampleResponse> getMethodWithNextAfterCloseMethod() {
    io.grpc.MethodDescriptor<Sample.SampleRequest, Sample.SampleResponse> getMethodWithNextAfterCloseMethod;
    if ((getMethodWithNextAfterCloseMethod = SampleServiceGrpc.getMethodWithNextAfterCloseMethod) == null) {
      synchronized (SampleServiceGrpc.class) {
        if ((getMethodWithNextAfterCloseMethod = SampleServiceGrpc.getMethodWithNextAfterCloseMethod) == null) {
          SampleServiceGrpc.getMethodWithNextAfterCloseMethod = getMethodWithNextAfterCloseMethod =
              io.grpc.MethodDescriptor.<Sample.SampleRequest, Sample.SampleResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "MethodWithNextAfterClose"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Sample.SampleRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Sample.SampleResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SampleServiceMethodDescriptorSupplier("MethodWithNextAfterClose"))
              .build();
        }
      }
    }
    return getMethodWithNextAfterCloseMethod;
  }

  private static volatile io.grpc.MethodDescriptor<Sample.SampleRequest,
      Sample.SampleResponse> getMethodWithNextAfterErrorMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "MethodWithNextAfterError",
      requestType = Sample.SampleRequest.class,
      responseType = Sample.SampleResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Sample.SampleRequest,
      Sample.SampleResponse> getMethodWithNextAfterErrorMethod() {
    io.grpc.MethodDescriptor<Sample.SampleRequest, Sample.SampleResponse> getMethodWithNextAfterErrorMethod;
    if ((getMethodWithNextAfterErrorMethod = SampleServiceGrpc.getMethodWithNextAfterErrorMethod) == null) {
      synchronized (SampleServiceGrpc.class) {
        if ((getMethodWithNextAfterErrorMethod = SampleServiceGrpc.getMethodWithNextAfterErrorMethod) == null) {
          SampleServiceGrpc.getMethodWithNextAfterErrorMethod = getMethodWithNextAfterErrorMethod =
              io.grpc.MethodDescriptor.<Sample.SampleRequest, Sample.SampleResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "MethodWithNextAfterError"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Sample.SampleRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Sample.SampleResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SampleServiceMethodDescriptorSupplier("MethodWithNextAfterError"))
              .build();
        }
      }
    }
    return getMethodWithNextAfterErrorMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static SampleServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SampleServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SampleServiceStub>() {
        @Override
        public SampleServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SampleServiceStub(channel, callOptions);
        }
      };
    return SampleServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static SampleServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SampleServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SampleServiceBlockingStub>() {
        @Override
        public SampleServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SampleServiceBlockingStub(channel, callOptions);
        }
      };
    return SampleServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static SampleServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SampleServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SampleServiceFutureStub>() {
        @Override
        public SampleServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SampleServiceFutureStub(channel, callOptions);
        }
      };
    return SampleServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * Sample service interface.
   * </pre>
   */
  public static abstract class SampleServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Method that always times out.
     * </pre>
     */
    public void methodThatTakesTooLong(Sample.SampleRequest request,
                                       io.grpc.stub.StreamObserver<Sample.SampleResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getMethodThatTakesTooLongMethod(), responseObserver);
    }

    /**
     * <pre>
     * Method that throws a given error.
     * </pre>
     */
    public void methodThatErrors(Sample.StatusRequest request,
                                 io.grpc.stub.StreamObserver<Sample.SampleResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getMethodThatErrorsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Method that affixes response trailers.
     * </pre>
     */
    public void methodWithTrailers(Sample.SampleRequest request,
                                   io.grpc.stub.StreamObserver<Sample.SampleResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getMethodWithTrailersMethod(), responseObserver);
    }

    /**
     * <pre>
     * Method that throws a non-gRPC fatal error.
     * </pre>
     */
    public void methodWithFatalError(Sample.SampleRequest request,
                                     io.grpc.stub.StreamObserver<Sample.SampleResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getMethodWithFatalErrorMethod(), responseObserver);
    }

    /**
     * <pre>
     * Method that calls `onNext` multiple times.
     * </pre>
     */
    public void methodWithMultipleResponses(Sample.SampleRequest request,
                                            io.grpc.stub.StreamObserver<Sample.SampleResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getMethodWithMultipleResponsesMethod(), responseObserver);
    }

    /**
     * <pre>
     * Method that calls `onNext` after `onCompleted`, which is illegal.
     * </pre>
     */
    public void methodWithNextAfterClose(Sample.SampleRequest request,
                                         io.grpc.stub.StreamObserver<Sample.SampleResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getMethodWithNextAfterCloseMethod(), responseObserver);
    }

    /**
     * <pre>
     * Method that calls `onNext` after `onError`, which is illegal.
     * </pre>
     */
    public void methodWithNextAfterError(Sample.SampleRequest request,
                                         io.grpc.stub.StreamObserver<Sample.SampleResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getMethodWithNextAfterErrorMethod(), responseObserver);
    }

    @Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getMethodThatTakesTooLongMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                Sample.SampleRequest,
                Sample.SampleResponse>(
                  this, METHODID_METHOD_THAT_TAKES_TOO_LONG)))
          .addMethod(
            getMethodThatErrorsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                Sample.StatusRequest,
                Sample.SampleResponse>(
                  this, METHODID_METHOD_THAT_ERRORS)))
          .addMethod(
            getMethodWithTrailersMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                Sample.SampleRequest,
                Sample.SampleResponse>(
                  this, METHODID_METHOD_WITH_TRAILERS)))
          .addMethod(
            getMethodWithFatalErrorMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                Sample.SampleRequest,
                Sample.SampleResponse>(
                  this, METHODID_METHOD_WITH_FATAL_ERROR)))
          .addMethod(
            getMethodWithMultipleResponsesMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                Sample.SampleRequest,
                Sample.SampleResponse>(
                  this, METHODID_METHOD_WITH_MULTIPLE_RESPONSES)))
          .addMethod(
            getMethodWithNextAfterCloseMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                Sample.SampleRequest,
                Sample.SampleResponse>(
                  this, METHODID_METHOD_WITH_NEXT_AFTER_CLOSE)))
          .addMethod(
            getMethodWithNextAfterErrorMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                Sample.SampleRequest,
                Sample.SampleResponse>(
                  this, METHODID_METHOD_WITH_NEXT_AFTER_ERROR)))
          .build();
    }
  }

  /**
   * <pre>
   * Sample service interface.
   * </pre>
   */
  public static final class SampleServiceStub extends io.grpc.stub.AbstractAsyncStub<SampleServiceStub> {
    private SampleServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected SampleServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SampleServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Method that always times out.
     * </pre>
     */
    public void methodThatTakesTooLong(Sample.SampleRequest request,
                                       io.grpc.stub.StreamObserver<Sample.SampleResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getMethodThatTakesTooLongMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Method that throws a given error.
     * </pre>
     */
    public void methodThatErrors(Sample.StatusRequest request,
                                 io.grpc.stub.StreamObserver<Sample.SampleResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getMethodThatErrorsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Method that affixes response trailers.
     * </pre>
     */
    public void methodWithTrailers(Sample.SampleRequest request,
                                   io.grpc.stub.StreamObserver<Sample.SampleResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getMethodWithTrailersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Method that throws a non-gRPC fatal error.
     * </pre>
     */
    public void methodWithFatalError(Sample.SampleRequest request,
                                     io.grpc.stub.StreamObserver<Sample.SampleResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getMethodWithFatalErrorMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Method that calls `onNext` multiple times.
     * </pre>
     */
    public void methodWithMultipleResponses(Sample.SampleRequest request,
                                            io.grpc.stub.StreamObserver<Sample.SampleResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getMethodWithMultipleResponsesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Method that calls `onNext` after `onCompleted`, which is illegal.
     * </pre>
     */
    public void methodWithNextAfterClose(Sample.SampleRequest request,
                                         io.grpc.stub.StreamObserver<Sample.SampleResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getMethodWithNextAfterCloseMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Method that calls `onNext` after `onError`, which is illegal.
     * </pre>
     */
    public void methodWithNextAfterError(Sample.SampleRequest request,
                                         io.grpc.stub.StreamObserver<Sample.SampleResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getMethodWithNextAfterErrorMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * Sample service interface.
   * </pre>
   */
  public static final class SampleServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<SampleServiceBlockingStub> {
    private SampleServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected SampleServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SampleServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Method that always times out.
     * </pre>
     */
    public Sample.SampleResponse methodThatTakesTooLong(Sample.SampleRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getMethodThatTakesTooLongMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Method that throws a given error.
     * </pre>
     */
    public Sample.SampleResponse methodThatErrors(Sample.StatusRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getMethodThatErrorsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Method that affixes response trailers.
     * </pre>
     */
    public Sample.SampleResponse methodWithTrailers(Sample.SampleRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getMethodWithTrailersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Method that throws a non-gRPC fatal error.
     * </pre>
     */
    public Sample.SampleResponse methodWithFatalError(Sample.SampleRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getMethodWithFatalErrorMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Method that calls `onNext` multiple times.
     * </pre>
     */
    public Sample.SampleResponse methodWithMultipleResponses(Sample.SampleRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getMethodWithMultipleResponsesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Method that calls `onNext` after `onCompleted`, which is illegal.
     * </pre>
     */
    public Sample.SampleResponse methodWithNextAfterClose(Sample.SampleRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getMethodWithNextAfterCloseMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Method that calls `onNext` after `onError`, which is illegal.
     * </pre>
     */
    public Sample.SampleResponse methodWithNextAfterError(Sample.SampleRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getMethodWithNextAfterErrorMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * Sample service interface.
   * </pre>
   */
  public static final class SampleServiceFutureStub extends io.grpc.stub.AbstractFutureStub<SampleServiceFutureStub> {
    private SampleServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected SampleServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SampleServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Method that always times out.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Sample.SampleResponse> methodThatTakesTooLong(
        Sample.SampleRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getMethodThatTakesTooLongMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Method that throws a given error.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Sample.SampleResponse> methodThatErrors(
        Sample.StatusRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getMethodThatErrorsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Method that affixes response trailers.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Sample.SampleResponse> methodWithTrailers(
        Sample.SampleRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getMethodWithTrailersMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Method that throws a non-gRPC fatal error.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Sample.SampleResponse> methodWithFatalError(
        Sample.SampleRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getMethodWithFatalErrorMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Method that calls `onNext` multiple times.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Sample.SampleResponse> methodWithMultipleResponses(
        Sample.SampleRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getMethodWithMultipleResponsesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Method that calls `onNext` after `onCompleted`, which is illegal.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Sample.SampleResponse> methodWithNextAfterClose(
        Sample.SampleRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getMethodWithNextAfterCloseMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Method that calls `onNext` after `onError`, which is illegal.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Sample.SampleResponse> methodWithNextAfterError(
        Sample.SampleRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getMethodWithNextAfterErrorMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_METHOD_THAT_TAKES_TOO_LONG = 0;
  private static final int METHODID_METHOD_THAT_ERRORS = 1;
  private static final int METHODID_METHOD_WITH_TRAILERS = 2;
  private static final int METHODID_METHOD_WITH_FATAL_ERROR = 3;
  private static final int METHODID_METHOD_WITH_MULTIPLE_RESPONSES = 4;
  private static final int METHODID_METHOD_WITH_NEXT_AFTER_CLOSE = 5;
  private static final int METHODID_METHOD_WITH_NEXT_AFTER_ERROR = 6;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final SampleServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(SampleServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_METHOD_THAT_TAKES_TOO_LONG:
          serviceImpl.methodThatTakesTooLong((Sample.SampleRequest) request,
              (io.grpc.stub.StreamObserver<Sample.SampleResponse>) responseObserver);
          break;
        case METHODID_METHOD_THAT_ERRORS:
          serviceImpl.methodThatErrors((Sample.StatusRequest) request,
              (io.grpc.stub.StreamObserver<Sample.SampleResponse>) responseObserver);
          break;
        case METHODID_METHOD_WITH_TRAILERS:
          serviceImpl.methodWithTrailers((Sample.SampleRequest) request,
              (io.grpc.stub.StreamObserver<Sample.SampleResponse>) responseObserver);
          break;
        case METHODID_METHOD_WITH_FATAL_ERROR:
          serviceImpl.methodWithFatalError((Sample.SampleRequest) request,
              (io.grpc.stub.StreamObserver<Sample.SampleResponse>) responseObserver);
          break;
        case METHODID_METHOD_WITH_MULTIPLE_RESPONSES:
          serviceImpl.methodWithMultipleResponses((Sample.SampleRequest) request,
              (io.grpc.stub.StreamObserver<Sample.SampleResponse>) responseObserver);
          break;
        case METHODID_METHOD_WITH_NEXT_AFTER_CLOSE:
          serviceImpl.methodWithNextAfterClose((Sample.SampleRequest) request,
              (io.grpc.stub.StreamObserver<Sample.SampleResponse>) responseObserver);
          break;
        case METHODID_METHOD_WITH_NEXT_AFTER_ERROR:
          serviceImpl.methodWithNextAfterError((Sample.SampleRequest) request,
              (io.grpc.stub.StreamObserver<Sample.SampleResponse>) responseObserver);
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

  private static abstract class SampleServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    SampleServiceBaseDescriptorSupplier() {}

    @Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return Sample.getDescriptor();
    }

    @Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("SampleService");
    }
  }

  private static final class SampleServiceFileDescriptorSupplier
      extends SampleServiceBaseDescriptorSupplier {
    SampleServiceFileDescriptorSupplier() {}
  }

  private static final class SampleServiceMethodDescriptorSupplier
      extends SampleServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    SampleServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (SampleServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new SampleServiceFileDescriptorSupplier())
              .addMethod(getMethodThatTakesTooLongMethod())
              .addMethod(getMethodThatErrorsMethod())
              .addMethod(getMethodWithTrailersMethod())
              .addMethod(getMethodWithFatalErrorMethod())
              .addMethod(getMethodWithMultipleResponsesMethod())
              .addMethod(getMethodWithNextAfterCloseMethod())
              .addMethod(getMethodWithNextAfterErrorMethod())
              .build();
        }
      }
    }
    return result;
  }
}
