package org.dreamlab.gRPCHandler;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.42.0)",
    comments = "Source: metadata.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class JavaConnGrpc {

  private JavaConnGrpc() {}

  public static final String SERVICE_NAME = "org.dreamlab.gRPCHandler.JavaConn";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<org.dreamlab.gRPCHandler.BatchMetadata,
      org.dreamlab.gRPCHandler.Ack> getNewBatchMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "NewBatch",
      requestType = org.dreamlab.gRPCHandler.BatchMetadata.class,
      responseType = org.dreamlab.gRPCHandler.Ack.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.dreamlab.gRPCHandler.BatchMetadata,
      org.dreamlab.gRPCHandler.Ack> getNewBatchMethod() {
    io.grpc.MethodDescriptor<org.dreamlab.gRPCHandler.BatchMetadata, org.dreamlab.gRPCHandler.Ack> getNewBatchMethod;
    if ((getNewBatchMethod = JavaConnGrpc.getNewBatchMethod) == null) {
      synchronized (JavaConnGrpc.class) {
        if ((getNewBatchMethod = JavaConnGrpc.getNewBatchMethod) == null) {
          JavaConnGrpc.getNewBatchMethod = getNewBatchMethod =
              io.grpc.MethodDescriptor.<org.dreamlab.gRPCHandler.BatchMetadata, org.dreamlab.gRPCHandler.Ack>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "NewBatch"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.dreamlab.gRPCHandler.BatchMetadata.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.dreamlab.gRPCHandler.Ack.getDefaultInstance()))
              .setSchemaDescriptor(new JavaConnMethodDescriptorSupplier("NewBatch"))
              .build();
        }
      }
    }
    return getNewBatchMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static JavaConnStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<JavaConnStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<JavaConnStub>() {
        @java.lang.Override
        public JavaConnStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new JavaConnStub(channel, callOptions);
        }
      };
    return JavaConnStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static JavaConnBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<JavaConnBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<JavaConnBlockingStub>() {
        @java.lang.Override
        public JavaConnBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new JavaConnBlockingStub(channel, callOptions);
        }
      };
    return JavaConnBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static JavaConnFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<JavaConnFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<JavaConnFutureStub>() {
        @java.lang.Override
        public JavaConnFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new JavaConnFutureStub(channel, callOptions);
        }
      };
    return JavaConnFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class JavaConnImplBase implements io.grpc.BindableService {

    /**
     */
    public void newBatch(org.dreamlab.gRPCHandler.BatchMetadata request,
        io.grpc.stub.StreamObserver<org.dreamlab.gRPCHandler.Ack> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getNewBatchMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getNewBatchMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                org.dreamlab.gRPCHandler.BatchMetadata,
                org.dreamlab.gRPCHandler.Ack>(
                  this, METHODID_NEW_BATCH)))
          .build();
    }
  }

  /**
   */
  public static final class JavaConnStub extends io.grpc.stub.AbstractAsyncStub<JavaConnStub> {
    private JavaConnStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected JavaConnStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new JavaConnStub(channel, callOptions);
    }

    /**
     */
    public void newBatch(org.dreamlab.gRPCHandler.BatchMetadata request,
        io.grpc.stub.StreamObserver<org.dreamlab.gRPCHandler.Ack> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getNewBatchMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class JavaConnBlockingStub extends io.grpc.stub.AbstractBlockingStub<JavaConnBlockingStub> {
    private JavaConnBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected JavaConnBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new JavaConnBlockingStub(channel, callOptions);
    }

    /**
     */
    public org.dreamlab.gRPCHandler.Ack newBatch(org.dreamlab.gRPCHandler.BatchMetadata request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getNewBatchMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class JavaConnFutureStub extends io.grpc.stub.AbstractFutureStub<JavaConnFutureStub> {
    private JavaConnFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected JavaConnFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new JavaConnFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.dreamlab.gRPCHandler.Ack> newBatch(
        org.dreamlab.gRPCHandler.BatchMetadata request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getNewBatchMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_NEW_BATCH = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final JavaConnImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(JavaConnImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_NEW_BATCH:
          serviceImpl.newBatch((org.dreamlab.gRPCHandler.BatchMetadata) request,
              (io.grpc.stub.StreamObserver<org.dreamlab.gRPCHandler.Ack>) responseObserver);
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

  private static abstract class JavaConnBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    JavaConnBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.dreamlab.gRPCHandler.Metadata.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("JavaConn");
    }
  }

  private static final class JavaConnFileDescriptorSupplier
      extends JavaConnBaseDescriptorSupplier {
    JavaConnFileDescriptorSupplier() {}
  }

  private static final class JavaConnMethodDescriptorSupplier
      extends JavaConnBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    JavaConnMethodDescriptorSupplier(String methodName) {
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
      synchronized (JavaConnGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new JavaConnFileDescriptorSupplier())
              .addMethod(getNewBatchMethod())
              .build();
        }
      }
    }
    return result;
  }
}
