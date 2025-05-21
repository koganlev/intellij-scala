package member

trait Type {
  type Abstract

  type Alias = Int

  /**/opaque /**/type Opaque/**/ = String/**/

  opaque type OpaqueReifiable = Int
}