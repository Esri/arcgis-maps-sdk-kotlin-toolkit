# Authentication Microapp

The Authentication Microapp demonstrates the use of the `Authenticator` component. The app also provides a testbed for testing different authentication challenges. For more information on the `Authenticator` component, see the [Readme]().

![](screenshot.png)

## Usage

On startup, the app presents the user with a text field containing a portal url. When the user presses load, the portal at the url in the field will be loaded and issue an authentication challenge (if required). The info screen below will display the portal info if the portal was successfully loaded, or the error if it failed to load.

The "Use OAuth" checkbox controls whether the app will configure the `Authenticator` to use OAuth (by setting the `AuthenticatorState.oAuthUserConfiguration`) or not. The "Signout" button clears any saved credentials.

To use OAuth sign in with a different URL than `https://www.arcgis.com`, the `AuthenticationAppViewModel.oAuthUserSignInConfiguration` should be changed:

```kotlin
private val oAuthUserConfiguration = OAuthUserConfiguration(
	<your-url-here>,
	// This client ID is for demo purposes only. For use of the Authenticator in your own app,
	// create your own client ID. For more info see: https://developers.arcgis.com/documentation/mapping-apis-and-services/security/tutorials/register-your-application/
	"lgAdHkYZYlwwfAhC",
	"my-ags-app://auth"
)
```


