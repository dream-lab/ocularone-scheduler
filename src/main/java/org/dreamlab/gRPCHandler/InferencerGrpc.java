package org.dreamlab.gRPCHandler;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.42.0)",
    comments = "Source: metadata.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class InferencerGrpc {

  private InferencerGrpc() {}

  public static final String SERVICE_NAME = "org.dreamlab.gRPCHandler.Inferencer";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<org.dreamlab.gRPCHandler.JobDetails,
      org.dreamlab.gRPCHandler.Ack> getSubmitMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Submit",
      requestType = org.dreamlab.gRPCHandler.JobDetails.class,
      responseType = org.dreamlab.gRPCHandler.Ack.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.dreamlab.gRPCHandler.JobDetails,
      org.dreamlab.gRPCHandler.Ack> getSubmitMethod() {
    io.grpc.MethodDescriptor<org.dreamlab.gRPCHandler.JobDetails, org.dreamlab.gRPCHandler.Ack> getSubmitMethod;
    if ((getSubmitMethod = InferencerGrpc.getSubmitMethod) == null) {
      synchronized (InferencerGrpc.class) {
        if ((getSubmitMethod = InferencerGrpc.getSubmitMethod) == null) {
          InferencerGrpc.getSubmitMethod = getSubmitMethod =
              io.grpc.MethodDescriptor.<org.dreamlab.gRPCHandler.JobDetails, org.dreamlab.gRPCHandler.Ack>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Submit"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.dreamlab.gRPCHandler.JobDetails.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.dreamlab.gRPCHandler.Ack.getDefaultInstance()))
              .setSchemaDescriptor(new InferencerMethodDescriptorSupplier("Submit"))
              .build();
        }
      }
    }
    return getSubmitMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static InferencerStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<InferencerStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<InferencerStub>() {
        @java.lang.Override
        public InferencerStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new InferencerStub(channel, callOptions);
        }
      };
    return InferencerStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static InferencerBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<InferencerBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<InferencerBlockingStub>() {
        @java.lang.Override
        public InferencerBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new InferencerBlockingStub(channel, callOptions);
        }
      };
    return InferencerBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static InferencerFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<InferencerFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<InferencerFutureStub>() {
        @java.lang.Override
        public InferencerFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new InferencerFutureStub(channel, callOptions);
        }
      };
    return InferencerFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class InferencerImplBase implements io.grpc.BindableService {

    /**
     */
    public void submit(org.dreamlab.gRPCHandler.JobDetails request,
        io.grpc.stub.StreamObserver<org.dreamlab.gRPCHandler.Ack> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSubmitMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getSubmitMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                org.dreamlab.gRPCHandler.JobDetails,
                org.dreamlab.gRPCHandler.Ack>(
                  this, METHODID_SUBMIT)))
          .build();
    }
  }

  /**
   */
  public static final class InferencerStub extends io.grpc.stub.AbstractAsyncStub<InferencerStub> {
    private InferencerStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected InferencerStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new InferencerStub(channel, callOptions);
    }

    /**
     */
    public void submit(org.dreamlab.gRPCHandler.JobDetails request,
        io.grpc.stub.StreamObserver<org.dreamlab.gRPCHandler.Ack> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSubmitMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class InferencerBlockingStub extends io.grpc.stub.AbstractBlockingStub<InferencerBlockingStub> {
    private InferencerBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected InferencerBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new InferencerBlockingStub(channel, callOptions);
    }

    /**
     */
    public org.dreamlab.gRPCHandler.Ack submit(org.dreamlab.gRPCHandler.JobDetails request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSubmitMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class InferencerFutureStub extends io.grpc.stub.AbstractFutureStub<InferencerFutureStub> {
    private InferencerFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected InferencerFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new InferencerFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.dreamlab.gRPCHandler.Ack> submit(
        org.dreamlab.gRPCHandler.JobDetails request) {
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
    private final InferencerImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(InferencerImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SUBMIT:
          serviceImpl.submit((org.dreamlab.gRPCHandler.JobDetails) request,
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

  private static abstract class InferencerBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    InferencerBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.dreamlab.gRPCHandler.Metadata.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Inferencer");
    }
  }

  private static final class InferencerFileDescriptorSupplier
      extends InferencerBaseDescriptorSupplier {
    InferencerFileDescriptorSupplier() {}
  }

  private static final class InferencerMethodDescriptorSupplier
      extends InferencerBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    InferencerMethodDescriptorSupplier(String methodName) {
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
      synchronized (InferencerGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new InferencerFileDescriptorSupplier())
              .addMethod(getSubmitMethod())
              .build();
        }
      }
    }
    return result;
  }
}
