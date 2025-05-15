package org.dreamlab.ApplicationDetails.GrpcGeneratedFiles;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.42.0)",
    comments = "Source: userapplicationdata.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class UserApplicationGrpc {

  private UserApplicationGrpc() {}

  public static final String SERVICE_NAME = "org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.UserApplication";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.UserApplicationInput,
      org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.Ack> getAddUserInputMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "addUserInput",
      requestType = org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.UserApplicationInput.class,
      responseType = org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.Ack.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.UserApplicationInput,
      org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.Ack> getAddUserInputMethod() {
    io.grpc.MethodDescriptor<org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.UserApplicationInput, org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.Ack> getAddUserInputMethod;
    if ((getAddUserInputMethod = UserApplicationGrpc.getAddUserInputMethod) == null) {
      synchronized (UserApplicationGrpc.class) {
        if ((getAddUserInputMethod = UserApplicationGrpc.getAddUserInputMethod) == null) {
          UserApplicationGrpc.getAddUserInputMethod = getAddUserInputMethod =
              io.grpc.MethodDescriptor.<org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.UserApplicationInput, org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.Ack>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "addUserInput"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.UserApplicationInput.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.Ack.getDefaultInstance()))
              .setSchemaDescriptor(new UserApplicationMethodDescriptorSupplier("addUserInput"))
              .build();
        }
      }
    }
    return getAddUserInputMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static UserApplicationStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<UserApplicationStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<UserApplicationStub>() {
        @java.lang.Override
        public UserApplicationStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new UserApplicationStub(channel, callOptions);
        }
      };
    return UserApplicationStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static UserApplicationBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<UserApplicationBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<UserApplicationBlockingStub>() {
        @java.lang.Override
        public UserApplicationBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new UserApplicationBlockingStub(channel, callOptions);
        }
      };
    return UserApplicationBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static UserApplicationFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<UserApplicationFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<UserApplicationFutureStub>() {
        @java.lang.Override
        public UserApplicationFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new UserApplicationFutureStub(channel, callOptions);
        }
      };
    return UserApplicationFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class UserApplicationImplBase implements io.grpc.BindableService {

    /**
     */
    public void addUserInput(org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.UserApplicationInput request,
        io.grpc.stub.StreamObserver<org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.Ack> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAddUserInputMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getAddUserInputMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.UserApplicationInput,
                org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.Ack>(
                  this, METHODID_ADD_USER_INPUT)))
          .build();
    }
  }

  /**
   */
  public static final class UserApplicationStub extends io.grpc.stub.AbstractAsyncStub<UserApplicationStub> {
    private UserApplicationStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UserApplicationStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new UserApplicationStub(channel, callOptions);
    }

    /**
     */
    public void addUserInput(org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.UserApplicationInput request,
        io.grpc.stub.StreamObserver<org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.Ack> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAddUserInputMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class UserApplicationBlockingStub extends io.grpc.stub.AbstractBlockingStub<UserApplicationBlockingStub> {
    private UserApplicationBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UserApplicationBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new UserApplicationBlockingStub(channel, callOptions);
    }

    /**
     */
    public org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.Ack addUserInput(org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.UserApplicationInput request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAddUserInputMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class UserApplicationFutureStub extends io.grpc.stub.AbstractFutureStub<UserApplicationFutureStub> {
    private UserApplicationFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UserApplicationFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new UserApplicationFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.Ack> addUserInput(
        org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.UserApplicationInput request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAddUserInputMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_ADD_USER_INPUT = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final UserApplicationImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(UserApplicationImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_ADD_USER_INPUT:
          serviceImpl.addUserInput((org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.UserApplicationInput) request,
              (io.grpc.stub.StreamObserver<org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.Ack>) responseObserver);
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

  private static abstract class UserApplicationBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    UserApplicationBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.dreamlab.ApplicationDetails.GrpcGeneratedFiles.Userapplicationdata.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("UserApplication");
    }
  }

  private static final class UserApplicationFileDescriptorSupplier
      extends UserApplicationBaseDescriptorSupplier {
    UserApplicationFileDescriptorSupplier() {}
  }

  private static final class UserApplicationMethodDescriptorSupplier
      extends UserApplicationBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    UserApplicationMethodDescriptorSupplier(String methodName) {
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
      synchronized (UserApplicationGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new UserApplicationFileDescriptorSupplier())
              .addMethod(getAddUserInputMethod())
              .build();
        }
      }
    }
    return result;
  }
}
