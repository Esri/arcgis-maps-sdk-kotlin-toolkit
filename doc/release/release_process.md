# Release Process

#### Release of individual toolkit components

Most toolkit components will be releasable individually. We won't release the `composable-map` component yet.
Each toolkit component has its own module in the `toolkit` folder, with support for publishing to a Maven repo.

In addition, a thin script to release all components at once will be implemented.

#### BOM release

In addition to the release of individual components, a "Bill Of Materials" (BOM) is released which represents
a complete set of intercompatible toolkit components.

#### Release versioning

Though it is possible to provide different version numbers for each toolkit component in the BOM, we version all components and the BOM
under one single version. This implies that if one component needs to be updated, new versions for each will be updated. We
don't foresee this happening much if ever. We release the toolkit once per Program Increment (PI), releasing each component and the BOM.

The version is derived from the runtimecore build number, but is not fixed to the release of the SDK. Generally the toolkit version
number will trail the released version number by a couple days, or weeks, and will have a dependency on the newly released SDK version.

Like the SDK, when we have nominated a release candidate, we scrub the build number from its version by hand, leaving the major, minor, and micro versions.