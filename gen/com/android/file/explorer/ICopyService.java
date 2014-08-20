/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Users\\Administrator\\git\\FileExplorer\\src\\com\\android\\file\\explorer\\ICopyService.aidl
 */
package com.android.file.explorer;
public interface ICopyService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.android.file.explorer.ICopyService
{
private static final java.lang.String DESCRIPTOR = "com.android.file.explorer.ICopyService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.android.file.explorer.ICopyService interface,
 * generating a proxy if needed.
 */
public static com.android.file.explorer.ICopyService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.android.file.explorer.ICopyService))) {
return ((com.android.file.explorer.ICopyService)iin);
}
return new com.android.file.explorer.ICopyService.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_testFun:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.testFun(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_startCopy:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
boolean _arg1;
_arg1 = (0!=data.readInt());
this.startCopy(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_setCopyStatus:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.setCopyStatus(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getCopyStatus:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getCopyStatus();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_notificaFromActivity:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
this.notificaFromActivity(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.android.file.explorer.ICopyService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public void testFun(java.lang.String path) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(path);
mRemote.transact(Stub.TRANSACTION_testFun, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void startCopy(java.lang.String targetDir, boolean is_move) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(targetDir);
_data.writeInt(((is_move)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_startCopy, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void setCopyStatus(int status) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(status);
mRemote.transact(Stub.TRANSACTION_setCopyStatus, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public int getCopyStatus() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getCopyStatus, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void notificaFromActivity(boolean isrestart) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((isrestart)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_notificaFromActivity, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_testFun = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_startCopy = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_setCopyStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_getCopyStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_notificaFromActivity = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
}
public void testFun(java.lang.String path) throws android.os.RemoteException;
public void startCopy(java.lang.String targetDir, boolean is_move) throws android.os.RemoteException;
public void setCopyStatus(int status) throws android.os.RemoteException;
public int getCopyStatus() throws android.os.RemoteException;
public void notificaFromActivity(boolean isrestart) throws android.os.RemoteException;
}
