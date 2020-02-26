import pandas as pd
import plotly.graph_objs as go
import plotly
import os, fnmatch

files = fnmatch.filter(os.listdir("."), "*.matrix.txt")
for file in files:
    df = pd.read_csv(file, sep='\t', index_col=0)
    data = []
    for i in range(len(df.index)):
        tmpData = go.Scatter(
            x=df.columns,
            y=df.iloc[i],
            mode='lines',
            marker=dict(
                color='lightgrey',
            ),
            hoverinfo='none',
        )
        data.append(tmpData)
    meanM = go.Scatter(
        x=df.columns,
        y=df.mean(),
        mode='markers',
        marker=dict(
            color='transparent',
            size=12,
            line=dict(
                width=1,
                color='blue'
            )
        ),
        hoverinfo='x+y',
    )
    meanL = go.Scatter(
        x=df.columns,
        y=df.mean(),
        mode='lines',
        marker=dict(
            color='blue',
            size=12,
        ),
        hoverinfo='x+y',
    )
    data.append(meanL)
    data.append(meanM)
    layout = go.Layout(
        width=800,
        height=400,
        xaxis=dict(
            showgrid=False,
            ticks='outside',
            zeroline=False,
            showline=True,
            mirror=True,
            title="Samples",
        ),
        yaxis=dict(
            showgrid=False,
            zeroline=False,
            ticks='outside',
            showline=True,
            mirror=True,
            title="Expression",
        ),
        hovermode="closest",
        showlegend=False,
    )
    config = dict(
        displaylogo=False,
        showLink=False,
        # modeBarButtonsToRemove=["sendDataToCloud"],
    )
    fig = dict(data=data, layout=layout)
    a = plotly.offline.plot(fig, filename="basic-scatter.html", output_type="div", include_plotlyjs=False,
                            config=config)
    i = file.split(".")[0]
    f = open(i + '.div.txt', 'w')
    f.write(a)
    f.close()
