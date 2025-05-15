package org.dreamlab.gRPCHandler;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.48.1)",
    comments = "Source: metadata")
@io.grpc.stub.annotations.GrpcGenerated
public final class PostInferencerGrpc {

  private PostInferencerGrpc() {}

  public static final String SERVICE_NAME = "org.dreamlab.gRPCHandler.PostInferencer";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<org.dreamlab.gRPCHandler.OutputDetails,
      org.dreamlab.gRPCHandler.Empty> getSubmitMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Submit",
      requestType = org.dreamlab.gRPCHandler.OutputDetails.class,
      responseType = org.dreamlab.gRPCHandler.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.dreamlab.gRPCHandler.OutputDetails,
      org.dreamlab.gRPCHandler.Empty> getSubmitMethod() {
    io.grpc.MethodDescriptor<org.dreamlab.gRPCHandler.OutputDetails, org.dreamlab.gRPCHandler.Empty> getSubmitMethod;
    if ((getSubmitMethod = PostInferencerGrpc.getSubmitMethod) == null) {
      synchronized (PostInferencerGrpc.class) {
        if ((getSubmitMethod = PostInferencerGrpc.getSubmitMethod) == null) {
          PostInferencerGrpc.getSubmitMethod = getSubmitMethod =
              io.grpc.MethodDescriptor.<org.dreamlab.gRPCHandler.OutputDetails, org.dreamlab.gRPCHandler.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Submit"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.dreamlab.gRPCHandler.OutputDetails.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.dreamlab.gRPCHandler.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new PostInferencerMethodDescriptorSupplier("Submit"))
              .build();
        }
      }
    }
    return getSubmitMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PostInferencerStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PostInferencerStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PostInferencerStub>() {
        @java.lang.Override
        public PostInferencerStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PostInferencerStub(channel, callOptions);
        }
      };
    return PostInferencerStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PostInferencerBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PostInferencerBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PostInferencerBlockingStub>() {
        @java.lang.Override
        public PostInferencerBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PostInferencerBlockingStub(channel, callOptions);
        }
      };
    return PostInferencerBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static PostInferencerFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PostInferencerFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PostInferencerFutureStub>() {
        @java.lang.Override
        public PostInferencerFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PostInferencerFutureStub(channel, callOptions);
        }
      };
    return PostInferencerFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class PostInferencerImplBase implements io.grpc.BindableService {

    /**
     */
    public void submit(org.dreamlab.gRPCHandler.OutputDetails request,
        io.grpc.stub.StreamObserver<org.dreamlab.gRPCHandler.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSubmitMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getSubmitMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                org.dreamlab.gRPCHandler.OutputDetails,
                org.dreamlab.gRPCHandler.Empty>(
                  this, METHODID_SUBMIT)))
          .build();
    }
  }

  /**
   */
  public static final class PostInferencerStub extends io.grpc.stub.AbstractAsyncStub<PostInferencerStub> {
    private PostInferencerStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PostInferencerStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PostInferencerStub(channel, callOptions);
    }

    /**
     */
    public void submit(org.dreamlab.gRPCHandler.OutputDetails request,
        io.grpc.stub.StreamObserver<org.dreamlab.gRPCHandler.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSubmitMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class PostInferencerBlockingStub extends io.grpc.stub.AbstractBlockingStub<PostInferencerBlockingStub> {
    private PostInferencerBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PostInferencerBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PostInferencerBlockingStub(channel, callOptions);
    }

    /**
     */
    public org.dreamlab.gRPCHandler.Empty submit(org.dreamlab.gRPCHandler.OutputDetails request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSubmitMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class PostInferencerFutureStub extends io.grpc.stub.AbstractFutureStub<PostInferencerFutureStub> {
    private PostInferencerFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PostInferencerFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PostInferencerFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.dreamlab.gRPCHandler.Empty> submit(
        org.dreamlab.gRPCHandler.OutputDetails request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSubmitMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SUBMIT = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final PostInferencerImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(PostInferencerImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SUBMIT:
          serviceImpl.submit((org.dreamlab.gRPCHandler.OutputDetails) request,
              (io.grpc.stub.StreamObserver<org.dreamlab.gRPCHandler.Empty>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class PostInferencerBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    PostInferencerBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.dreamlab.gRPCHandler.Metadata.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("PostInferencer");
    }
  }

  private static final class PostInferencerFileDescriptorSupplier
      extends PostInferencerBaseDescriptorSupplier {
    PostInferencerFileDescriptorSupplier() {}
  }

  private static final class PostInferencerMethodDescriptorSupplier
      extends PostInferencerBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    PostInferencerMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (PostInferencerGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new PostInferencerFileDescriptorSupplier())
              .addMethod(getSubmitMethod())
              .build();
        }
      }
    }
    return result;
  }
}
