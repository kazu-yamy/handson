import MapKit
import SwiftData
import SwiftUI

struct TodoDetailView: View {
    // 保存を明示的に行うための窓口
    @Environment(\.modelContext) private var modelContext
    let record: TodoRecord

    // 地図の表示位置。初期値は場所があればそこ
    @State private var position: MapCameraPosition

    init(record: TodoRecord) {
        self.record = record
        if let latitude = record.latitude, let longitude = record.longitude {
            let center = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
            _position = State(initialValue: .region(
                MKCoordinateRegion(center: center, latitudinalMeters: 1000, longitudinalMeters: 1000)
            ))
        } else {
            // 場所が無いときは東京駅あたりを中心にしておく
            let tokyo = CLLocationCoordinate2D(latitude: 35.681236, longitude: 139.767125)
            _position = State(initialValue: .region(
                MKCoordinateRegion(center: tokyo, latitudinalMeters: 5000, longitudinalMeters: 5000)
            ))
        }
    }

    // 緯度と経度がそろっているときだけ座標になる
    private var coordinate: CLLocationCoordinate2D? {
        guard let latitude = record.latitude, let longitude = record.longitude else { return nil }
        return CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
    }

    var body: some View {
        List {
            LabeledContent("タイトル", value: record.title)
            LabeledContent("優先度", value: record.priority.label)
            LabeledContent("状態", value: record.isDone ? "完了" : "未完了")
            if let date = record.completedAt {
                LabeledContent("完了日時") {
                    Text(date, format: .dateTime.year().month().day().hour().minute())
                }
            }

            Section {
                MapReader { proxy in
                    Map(position: $position) {
                        if let coordinate {
                            Marker(record.title, coordinate: coordinate)
                        }
                    }
                    // タップした画面上の位置（ローカル座標）を、地図上の緯度経度に変換する
                    .onTapGesture { point in
                        if let tapped = proxy.convert(point, from: .local) {
                            record.setLocation(latitude: tapped.latitude, longitude: tapped.longitude)
                            // 自動保存を待たず、設定の直後に保存する
                            try? modelContext.save()
                        }
                    }
                }
                .frame(height: 280)

                if let coordinate {
                    Text("緯度 \(coordinate.latitude, specifier: "%.4f") / 経度 \(coordinate.longitude, specifier: "%.4f")")
                        .foregroundStyle(.secondary)
                    Button("場所を消す", role: .destructive) {
                        record.clearLocation()
                        try? modelContext.save()
                    }
                } else {
                    Text("地図をタップして場所を設定")
                        .foregroundStyle(.secondary)
                }
            } header: {
                Text("場所")
            }
        }
        .navigationTitle(record.title)
        .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview("場所あり") {
    let record = TodoRecord(title: "東京駅で待ち合わせ")
    record.latitude = 35.681236
    record.longitude = 139.767125
    return NavigationStack { TodoDetailView(record: record) }
}

#Preview("場所なし") {
    NavigationStack { TodoDetailView(record: TodoRecord(title: "牛乳を買う")) }
}
