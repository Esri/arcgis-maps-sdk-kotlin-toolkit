# FeatureForms Micro-app

This micro-app demonstrates the use of the [FeatureForm](../../toolkit/featureforms/README.md) toolkit component which provides a rich, dynamic, and responsive form
for editing Feature attributes.

![Screenshot](screenshot2.png) ![Screenshot](screenshot3.png)

## Usage

The application provides a web map gallery and a map viewer which invokes the form when Features are tapped.
Authentication is optional but limited to specific public web maps.

To authenticate ArcGIS Online with username and password, please provide values in `local.properties` as follows

```
webMapUser=XXX
webMapPassword=YYY
```

For more information on the `FeatureForm` component and how it works, see it's [Readme](../../toolkit/featureforms/README.md).
