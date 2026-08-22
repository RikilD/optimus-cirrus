package optimus.platform.dsi.bitemporal.proto

/**
 * Compile-time stand-in for the generated `peer.proto` messages.
 *
 * The DAL protobuf schemas (dsi.proto, peer.proto, prc.proto, expressions.proto) are not part of the
 * open-source export, so the generated classes cannot be produced here. See BOOTSTRAP.md.
 */
object Peer {
  class EntityTimeSliceReferenceProto extends Dsi.MessageLiteImpl
}
