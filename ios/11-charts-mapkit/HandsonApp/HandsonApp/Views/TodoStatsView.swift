import Charts
import SwiftData
import SwiftUI

struct TodoStatsView: View {
    @Query private var records: [TodoRecord]

    private var stats: TodoStats { TodoStats(records: records) }

    // 状態と色の対応を 1 か所で決める（2 つのグラフで同じ色にそろえる）
    private let colors: KeyValuePairs<String, Color> = ["完了": .green, "未完了": .orange]

    var body: some View {
        List {
            Section("件数") {
                Chart(stats.entries) { entry in
                    BarMark(
                        x: .value("状態", entry.status),
                        y: .value("件数", entry.count)
                    )
                    .foregroundStyle(by: .value("状態", entry.status))
                    // 棒の上に件数を表示する
                    .annotation(position: .top) {
                        Text("\(entry.count)")
                            .font(.caption.bold())
                    }
                    .accessibilityLabel(entry.status)
                    .accessibilityValue("\(entry.count) 件")
                }
                .chartForegroundStyleScale(colors)
                // 件数は小さい整数なので、目盛りの数を 3 程度に抑える
                .chartYAxis {
                    AxisMarks(values: .automatic(desiredCount: 3)) { _ in
                        AxisGridLine()
                        AxisValueLabel()
                    }
                }
                // X 軸は凡例と重複するので隠す
                .chartXAxis(.hidden)
                .chartLegend(position: .bottom, alignment: .center)
                .frame(height: 220)
            }

            Section("割合") {
                Chart(stats.entries) { entry in
                    SectorMark(
                        angle: .value("件数", entry.count),
                        innerRadius: .ratio(0.6),  // 内側をくり抜いてドーナツにする
                        angularInset: 2
                    )
                    .foregroundStyle(by: .value("状態", entry.status))
                    .accessibilityLabel(entry.status)
                    .accessibilityValue("\(entry.count) 件")
                }
                .chartForegroundStyleScale(colors)
                .chartBackground { _ in
                    // ドーナツの中央に合計を出す
                    VStack {
                        Text("\(stats.total)")
                            .font(.title.bold())
                        Text("件")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
                .chartLegend(position: .bottom, alignment: .center)
                .frame(height: 240)
            }
        }
        .navigationTitle("統計")
        .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview {
    NavigationStack { TodoStatsView() }
        .modelContainer(for: TodoRecord.self, inMemory: true)
}
