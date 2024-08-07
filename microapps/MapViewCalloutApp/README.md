# MapView Callout Micro-app

This micro-app demonstrates the use of `Callout` with a composable `MapView`. `Callout` is a composable function which renders an empty composable Box placed at a given point or GeoElement on a Map. The content of the composable Box is customizable, while the container of the Box is a stylable rectangular shape with a leader line positioned at the point of the tap or GeoElement passed into the `Callout` composable functions.

## Usage

The application starts with a choice of two screens
* A `MapView` that displays a `Callout` at the location where the user taps on the screen.
* A `MapView` with `Callout`s placed on GeoElements tapped by the user.
* For more information on the composable `Callout` component and how it works, see the relevant section in the composable GeoView [Readme](../../toolkit/geoview-compose#display-a-callout).

![Callout-geoelement](https://github.com/user-attachments/assets/d6fd278a-c773-45f3-9ecd-a76852b71192)

