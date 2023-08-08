# Authentication Microapp

The Authentication Microapp demonstrates the use of the `Authenticator` component, which can also be used as a platform for experimenting with authentication related API. For more information on the `Authenticator` component and how it works, see the [Readme](../../toolkit/authentication/README.md).

![](screenshot.png)

## Usage

On startup, the app presents the user with a editable text field containing a portal URL. Upon pressing the "Load" button, a portal will be created and loaded. If the portal is secured, it may potentially issue an authentication challenge. 
If the portal is successfully loaded, the info screen below will display the portal info JSON, otherwise it will display the loading error. 

The "Use OAuth" checkbox controls whether the `Authenticator` will use OAuth to load the `Portal`.
The "Signout" button clears any saved credentials.

To load a different Portal URL with OAuth, the `AuthenticationAppViewModel.oAuthUserConfiguration` should be changed to match the portal's configuration. 

```kotlin
private val oAuthUserConfiguration = OAuthUserConfiguration(
	<your-url-here>,
	// This client ID is for demo purposes only. For use of the Authenticator in your own app,
	// create your own client ID. For more info see: https://developers.arcgis.com/documentation/mapping-apis-and-services/security/tutorials/register-your-application/
	"lgAdHkYZYlwwfAhC",
	"my-ags-app://auth"
)
```


