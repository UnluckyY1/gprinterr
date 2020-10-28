import 'dart:async';
import 'dart:convert';
import 'dart:ui' as ui;
import 'package:bluetooth_print/bluetooth_print.dart';
import 'package:bluetooth_print/bluetooth_print_model.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';


import 'label.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  BluetoothPrint bluetoothPrint = BluetoothPrint.instance;

  bool _connected = false;
  BluetoothDevice _device;
  String tips = 'Gprinter not connect';
  GlobalKey globalKey1 = GlobalKey();
  GlobalKey globalKey2 = GlobalKey();

  String pathImage;

  @override
  void initState() {
    super.initState();

    WidgetsBinding.instance.addPostFrameCallback((_) => initBluetooth());
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initBluetooth() async {
    bluetoothPrint.startScan(timeout: Duration(seconds: 4));

    bool isConnected = await bluetoothPrint.isConnected;

    bluetoothPrint.state.listen((state) {
      print('cur device status: $state');

      switch (state) {
        case BluetoothPrint.CONNECTED:
          setState(() {
            _connected = true;
            tips = 'connect success';
          });
          break;
        case BluetoothPrint.DISCONNECTED:
          setState(() {
            _connected = false;
            tips = 'disconnect success';
          });
          break;
        default:
          break;
      }
    });

    if (!mounted) return;

    if (isConnected) {
      setState(() {
        _connected = true;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Siyou Gprinter Dev'),
        ),
        body: RefreshIndicator(
          onRefresh: () =>
              bluetoothPrint.startScan(timeout: Duration(seconds: 4)),
          child: SingleChildScrollView(
            child: Column(
              children: <Widget>[
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: <Widget>[
                    Padding(
                      padding:
                          EdgeInsets.symmetric(vertical: 10, horizontal: 10),
                      child: Text(tips),
                    ),
                  ],
                ),
                Divider(),
                StreamBuilder<List<BluetoothDevice>>(
                  stream: bluetoothPrint.scanResults,
                  initialData: [],
                  builder: (c, snapshot) => Column(
                    children: snapshot.data
                        .map((d) => ListTile(
                              title: Text(d.name ?? ''),
                              subtitle: Text(d.address),
                              onTap: () async {
                                setState(() {
                                  _device = d;
                                });
                              },
                              trailing: _device != null &&
                                      _device.address == d.address
                                  ? Icon(
                                      Icons.check,
                                      color: Colors.green,
                                    )
                                  : null,
                            ))
                        .toList(),
                  ),
                ),
                Divider(),
                Container(
                  height: 224,
                  width: 360,
                  child: Center(
                    child: SizedBox(
                      height: 204,
                      //width: 466,
                      child: RepaintBoundary(
                          key: globalKey1,
                          child: Container(
                              color: Colors.white,
                              child: StandardProductLabel())),
                    ),
                  ),
                ),
                Container(
                  padding: EdgeInsets.fromLTRB(20, 5, 20, 10),
                  child: Column(
                    children: <Widget>[
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: <Widget>[
                          OutlineButton(
                            child: Text('connect'),
                            onPressed: _connected
                                ? null
                                : () async {
                                    if (_device != null &&
                                        _device.address != null) {
                                      await bluetoothPrint.connect(_device);
                                    } else {
                                      setState(() {
                                        tips = 'please select device';
                                      });
                                      print('please select device');
                                    }
                                  },
                          ),
                          SizedBox(width: 10.0),
                          OutlineButton(
                            child: Text('disconnect'),
                            onPressed: _connected
                                ? () async {
                                    await bluetoothPrint.disconnect();
                                  }
                                : null,
                          ),
                        ],
                      ),
                      OutlineButton(
                        child: Text('print normal label'),
                        onPressed: _connected
                            ? () async {
                                captture(false);
                              }
                            : null,
                      ),
                      OutlineButton(
                        child: Text('print small label'),
                        onPressed: _connected
                            ? () async {
                                captture(true);
                              }
                              : () async {captture(true);},
                      ),
                      OutlineButton(
                        child: Text('print blackmark label'),
                        onPressed: _connected
                            ? () async {
                                captture(null);
                              }
                              : () async {captture(true);},
                      ),
                    ],
                  ),
                )
              ],
            ),
          ),
        ),
        floatingActionButton: StreamBuilder<bool>(
          stream: bluetoothPrint.isScanning,
          initialData: false,
          builder: (c, snapshot) {
            if (snapshot.data) {
              return FloatingActionButton(
                child: Icon(Icons.stop),
                onPressed: () => bluetoothPrint.stopScan(),
                backgroundColor: Colors.red,
              );
            } else {
              return FloatingActionButton(
                  child: Icon(Icons.search),
                  onPressed: () =>
                      bluetoothPrint.startScan(timeout: Duration(seconds: 4)));
            }
          },
        ),
      ),
    );
  }

  Future<ByteData> captureWidgetPng(RenderRepaintBoundary boundary) async {
    ui.Image image = await boundary.toImage(pixelRatio: 2);
    ByteData byteData = await image.toByteData(format: ui.ImageByteFormat.png);

    return byteData;
  }

  void captture(bool mini) async {
    RenderRepaintBoundary boundary =
        globalKey1.currentContext.findRenderObject();
    final data = await captureWidgetPng(boundary);
    List<int> imageBytes =
        data.buffer.asUint8List(data.offsetInBytes, data.lengthInBytes);
    String base64Image = base64Encode(imageBytes);

    List<LineText> list = List();
    Map<String, dynamic> config = Map();
    config['width'] = 60; // 标签宽度，单位mm
    config['height'] = 40; 
    
    // 标签高度，单位mm
    if (mini!=null)
    config['gap'] = 2;
   

    list.add(LineText(
      //width: 10,
      // height: 10,
      align: 0,
      type: LineText.TYPE_IMAGE,
      x: 10,
      y: 10,
      content: base64Image,
    ));
    if(mini==null)
    await bluetoothPrint.printLabel(config, list,false);
    else if(mini)
    await bluetoothPrint.printLabel(config, list,true);
    else 
    await bluetoothPrint.printLabel(config, list,false);
    
  }
}
