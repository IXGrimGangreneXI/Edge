#!/bin/bash
source versions.info
rm -rf build/updatedata
if [ ! -d build/secrets ]; then
    1>&2 echo Error: missing build/secrets folder, need privatekey.pem and publickey.pem
    exit 1
fi

function copyUpdateData() {
	version="$1"
	ch="$2"
	package="$3"

    mkdir -p build/updatedata/projectedge.net/httpdocs/updates/$package || exit 1
    cp -rfv build/update/$package/$version build/updatedata/projectedge.net/httpdocs/updates/$package/$ch || exit 1
    echo -n "$version" > build/updatedata/projectedge.net/httpdocs/updates/$package/$ch/update.info
}

# Build
echo Building data...
./gradlew build updateData || exit $?

# Create output
echo Creating output
mkdir build/updatedata/projectedge.net/httpdocs -p
mkdir build/updatedata/sentinel.projectedge.net/httpdocs -p

# Sign sentinel package
echo Signing sentinel package...
mkdir build/updatedata/tmp
cp -rf build/secrets/. build/updatedata/tmp
cp build/sentinal-images-unsigned/edge-sentinel-emulation-software-$globalserver.svp build/updatedata/tmp/emulationsoftware.svp || exit 1
cp "deps/razorwhip/Sentinel Launcher/sentinel-launcher/build/libs/sentinel-launcher-$sentinel-all.jar" build/updatedata/tmp/sentinel.jar || exit 1
cd build/updatedata/tmp
java -cp sentinel.jar org.asf.razorwhip.sentinel.launcher.tools.SignPackageTool emulationsoftware.svp || exit 1
cd ../../..
mkdir -p build/updatedata/sentinel.projectedge.net/httpdocs/software/sod/projectedge
cp build/updatedata/tmp/emulationsoftware-signed.svp build/updatedata/sentinel.projectedge.net/httpdocs/software/sod/projectedge/edge-sentinel-emulation-software-$globalserver.svp || exit 1

# Copy update data
echo Copying globalserver update data...
copyUpdateData "$globalserver" "$channel" "globalserver"
if [ "$channel" == "unstable" ]; then
	copyUpdateData "$globalserver" dev "globalserver"
fi
if [ "$channel" == "stable" ]; then
	copyUpdateData "$globalserver" unstable "globalserver"
	copyUpdateData "$globalserver" dev "globalserver"
fi

echo Copying contentserver update data...
copyUpdateData "$contentserver" "$channel" "contentserver"
if [ "$channel" == "unstable" ]; then
	copyUpdateData "$contentserver" dev "contentserver"
fi
if [ "$channel" == "stable" ]; then
	copyUpdateData "$contentserver" unstable "contentserver"
	copyUpdateData "$contentserver" dev "contentserver"
fi


echo Copying commonapi update data...
copyUpdateData "$commonapi" "$channel" "commonapi"
if [ "$channel" == "unstable" ]; then
	copyUpdateData "$commonapi" dev "commonapi"
fi
if [ "$channel" == "stable" ]; then
	copyUpdateData "$commonapi" unstable "commonapi"
	copyUpdateData "$commonapi" dev "commonapi"
fi

echo Copying gameplayapi update data...
copyUpdateData "$gameplayapi" "$channel" "gameplayapi"
if [ "$channel" == "unstable" ]; then
	copyUpdateData "$gameplayapi" dev "gameplayapi"
fi
if [ "$channel" == "stable" ]; then
	copyUpdateData "$gameplayapi" unstable "gameplayapi"
	copyUpdateData "$gameplayapi" dev "gameplayapi"
fi

echo Copying mmoserver update data...
copyUpdateData "$mmoserver" "$channel" "mmoserver"
if [ "$channel" == "unstable" ]; then
	copyUpdateData "$mmoserver" dev "mmoserver"
fi
if [ "$channel" == "stable" ]; then
	copyUpdateData "$mmoserver" unstable "mmoserver"
	copyUpdateData "$mmoserver" dev "mmoserver"
fi

echo Downloading current version json...
curl https://sentinel.projectedge.net/software/sod/projectedge/updates.json -f --output build/updatedata/sentinel.projectedge.net/httpdocs/software/sod/projectedge/updates.json || exit 1
echo Modifying version json...
echo "$(cat build/updatedata/sentinel.projectedge.net/httpdocs/software/sod/projectedge/updates.json | jq '.latest = $v' --arg v $globalserver | jq '."'"$globalserver"'" |= {"url":"'"https://sentinel.projectedge.net/software/sod/projectedge/edge-sentinel-emulation-software-$globalserver.svp"'","hash":"'"$(sha256sum build/updatedata/sentinel.projectedge.net/httpdocs/software/sod/projectedge/edge-sentinel-emulation-software-$globalserver.svp | awk '{ print $1 }')"'"}')" > build/updatedata/sentinel.projectedge.net/httpdocs/software/sod/projectedge/updates.json
rm -rf build/updatedata/tmp
echo Done.
