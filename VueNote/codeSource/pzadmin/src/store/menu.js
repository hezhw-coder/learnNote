const state = {
    isCollapse: false,
    selectmenu: []
}

const mutations = {
    collapseMenu(state) {
        state.isCollapse = !state.isCollapse
    },
    addMenu(state, payload) {
        //对数据进行去重
        if (state.selectmenu.findIndex(item => item.path == payload.path) === -1) {
            state.selectmenu.push(payload)
        }
    },
    closeMenu(state, payload) {
        //找到点击数据的索引
        const index = state.selectmenu.findIndex(val => val.name === payload.name)
        state.selectmenu.splice(index, 1)
    }
}

export default {
    state,
    mutations
}