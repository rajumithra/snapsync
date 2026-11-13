# SnapSync – Camera Connectivity & Configuration Guide

SnapSync leverages direct camera-to-phone ingestion using standard camera protocols:
1. **Wi-Fi FTP Transfer** (Fastest, zero cable, automatic background relay upon shutter press)
2. **Camera Hotspot / Phone Hotspot Mode** (For outdoor, wedding, or field events without venue Wi-Fi)
3. **USB OTG PTP/MTP Mode** (For ultra-fast wired transfer or cameras without Wi-Fi FTP)

---

## 1. Network Topology Options

### Option A: Phone Hotspot Mode (Recommended for Outdoor / On-the-Move)
1. Turn ON the **Personal Hotspot** on your Android smartphone.
2. Connect your camera to the Android phone's Wi-Fi network.
3. Open SnapSync. The app automatically detects its local hotspot IP address (e.g. `192.168.43.1` or `192.168.1.1`).
4. SnapSync's Embedded FTP Server runs on port `2121`.

### Option B: Venue Wi-Fi / Portable 5G MiFi
1. Connect both the camera and the Android smartphone to the same Wi-Fi network or mobile 5G router.
2. Note the IP address shown on the SnapSync **Camera Connectivity** screen (e.g. `192.168.0.145`).

---

## 2. Sony Alpha Series Setup (A7 IV, A7R V, A1, A9 II/III, FX3)

Sony cameras natively support automated FTP background transfer while shooting.

1. **Network Connection**:
   - Menu -> **Network** -> **Wi-Fi** -> **Wi-Fi Connect** -> **On**.
   - Select **Access Point Set** -> Connect to your Android Hotspot or shared Wi-Fi.
2. **FTP Transfer Function**:
   - Menu -> **Network** -> **Transfer/Remote** -> **FTP Transfer Func.**:
     - **FTP Function**: `ON`
     - **Server Setting** -> Select **Server 1**:
       - **Display Name**: `SnapSync`
       - **Destination**:
         - **Host Name / IP Address**: Enter IP shown in SnapSync app (e.g. `192.168.43.1`)
         - **Port**: `2121`
         - **Directory**: `/`
       - **User Info**:
         - **User**: `snapsync`
         - **Password**: Enter password displayed in SnapSync (or default configured in settings)
         - **PASV Mode**: `ON`
3. **Auto Transfer Mode**:
   - Menu -> **Auto FTP Transfer**: `ON`
   - **Transfer Target**: `JPEG Only` (or `RAW & JPEG` / `RAW Only` depending on event bandwidth)
4. **Shoot**:
   - Take a shot. Look for the FTP arrow icon on the camera viewfinder. The green arrow indicates transmission. The photo appears in SnapSync within 1 second.

---

## 3. Canon EOS Setup (EOS R3, R5, R6 Mk II, 1D X Mk III)

1. **FTP Connection Wizard**:
   - Menu -> **Network** -> **FTP Transfer** -> **FTP Server**:
     - Select **Create with Wizard**.
     - Communication method: **Wi-Fi**.
     - Select network: Connect to Android hotspot.
     - IP Address Setting: **Auto (DHCP)**.
2. **FTP Server Settings**:
   - **FTP Mode**: `FTP` (Plain) or `FTPS` (if TLS configured).
   - **Target Server**: Enter Android IP (e.g. `192.168.43.1`).
   - **Port Number**: `2121`.
   - **Passive Mode**: `Enable`.
   - **Target Folder**: Root `/` or `snapsync`.
   - **Login Method**: Enter Username and Password shown on the SnapSync screen.
3. **Automatic Transfer**:
   - Set **Automatic Transfer during shooting**: `Enable`.
   - Set transfer format: `JPEG` or `JPEG+RAW`.

---

## 4. Nikon Z Series Setup (Z8, Z9, Z6 III)

1. **Connect to Smart Device / Network**:
   - Menu -> **Network Menu** -> **Connect to FTP Server**:
     - Select **Create Profile**.
     - Connection method: **Search for Wi-Fi network** -> Connect to Android hotspot.
     - IP Address: **Obtain automatically**.
2. **FTP Configuration**:
   - Server Type: **FTP**.
   - Address: Enter Android device IP address (e.g. `192.168.43.1`).
   - Port: `2121`.
   - Anonymous Login: `OFF`.
   - User ID & Password: as specified in SnapSync.
   - PASV Mode: `ON`.
3. **Auto Send Options**:
   - **Options** -> **Auto send**: `ON`.
   - Choose slot: Slot 1 or Slot 2.

---

## 5. USB OTG / PTP Mode (Wired Tethering)

For cameras lacking built-in Wi-Fi FTP or when shooting in high-interference RF environments:
1. Connect a USB-C to USB-C (or Micro-USB to USB-C OTG) cable between the camera and Android phone.
2. On the camera, set USB Connection Mode to **PC Remote**, **Tether**, or **MTP/PTP**.
3. Android will prompt: `Allow SnapSync to access USB device?` -> Tap **Allow**.
4. SnapSync listens for PTP ObjectAdded events and downloads newly captured images directly into the upload queue.

---

## 6. Verifying the Connection

In the SnapSync Android App:
- Open the **Camera Connectivity** tab.
- Observe:
  - **FTP Server Status**: Running (Green badge).
  - **Listening Interface**: e.g., `wlan0 (192.168.43.1:2121)`.
  - **Connected Clients**: Shows IP of camera when handshake initiates.
  - **Live Transfer Log**: Displays `STOR DSC00123.JPG` incoming stream and speed.

