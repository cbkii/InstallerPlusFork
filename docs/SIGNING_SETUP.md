# APK Signing Setup with GitHub Secrets

This repository uses GitHub Secrets to securely sign release APKs in CI/CD workflows.

## Prerequisites

You need a Java keystore file (`.jks` or `.keystore`) with your signing key. If you don't have one, create it:

```bash
keytool -genkey -v -keystore my-release-key.jks \
  -alias my-release-key -keyalg RSA -keysize 2048 -validity 10000
```

## Setting Up GitHub Secrets

### Step 1: Encode Your Keystore to Base64

On Linux/macOS:
```bash
base64 -i my-release-key.jks | tr -d '\n' > keystore.txt
```

On Windows (PowerShell):
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("my-release-key.jks")) | Out-File -FilePath keystore.txt -NoNewline
```

### Step 2: Add GitHub Repository Secrets

Go to your repository settings → Secrets and variables → Actions → New repository secret

Add these 4 secrets:

| Secret Name | Value | Description |
|------------|-------|-------------|
| `KEYSTORE_FILE` | Content of `keystore.txt` | Your keystore encoded as base64 |
| `KEYSTORE_PASSWORD` | Your keystore password | Password you used when creating the keystore |
| `KEY_ALIAS` | `my-release-key` | The alias you used (default: `my-release-key`) |
| `KEY_PASSWORD` | Your key password | Usually same as keystore password |

### Step 3: Trigger a Build

Once secrets are configured, push to `main` branch or trigger a workflow manually. The release APK will be signed automatically.

## How It Works

During the GitHub Actions workflow:
1. The base64-encoded keystore is decoded and saved as `app/my-release-key.jks`
2. A `keystore.properties` file is created with signing credentials
3. Gradle reads from `keystore.properties` to sign the release APK
4. Both the keystore and properties file are temporary and never committed

## Security Notes

- ⚠️ **Never commit your keystore file or passwords to the repository**
- ✅ GitHub Secrets are encrypted and only exposed to workflow runs
- ✅ Secrets are not available to pull request workflows from forks (security feature)
- ✅ The keystore file and properties are temporarily created during the build and deleted after

## Verifying Signed APKs

To verify an APK is signed:

```bash
# Check signature
apksigner verify --print-certs InstallerPlusFork-release-signed.apk

# Or using jarsigner
jarsigner -verify -verbose -certs InstallerPlusFork-release-signed.apk
```

## Finding Your Key Alias

If you don't remember your key alias, run:

```bash
keytool -list -v -keystore my-release-key.jks
```

Look for the "Alias name" in the output.

## Local Builds

Local release builds will be unsigned unless you create a `keystore.properties` file in the project root:

```properties
storeFile=/path/to/your/my-release-key.jks
storePassword=your_keystore_password
keyAlias=my-release-key
keyPassword=your_key_password
```

**Important:** Add `keystore.properties` to `.gitignore` (already configured) to prevent accidental commits.
